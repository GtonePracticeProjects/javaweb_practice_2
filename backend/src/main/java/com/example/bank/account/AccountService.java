package com.example.bank.account;

import com.example.bank.common.BusinessException;
import com.example.bank.member.Member;
import com.example.bank.member.MemberRepository;
import com.example.bank.member.MemberStatus;
import com.example.bank.transaction.AccountTransaction;
import com.example.bank.transaction.AccountTransactionRepository;
import com.example.bank.transaction.TransactionDto;
import com.example.bank.transaction.TransactionType;
import com.example.bank.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository txRepository;
    private final MemberRepository memberRepository;
    private final BigDecimal dailyTransferLimit;
    private final NotificationService notificationService;

    public AccountService(AccountRepository accountRepository,
                          AccountTransactionRepository txRepository,
                          MemberRepository memberRepository,
                          NotificationService notificationService,
                          @Value("${bank.transfer.daily-limit:10000000}") BigDecimal dailyTransferLimit) {
        this.accountRepository = accountRepository;
        this.txRepository = txRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.dailyTransferLimit = dailyTransferLimit;
    }

    // ---- accounts --------------------------------------------------------

    @Transactional
    public AccountDto.Response open(Long memberId, AccountDto.OpenRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw BusinessException.forbidden("계좌를 개설할 수 없는 회원 상태입니다.");
        }
        Account account = accountRepository.save(
                new Account(generateAccountNumber(), member, req.accountType(), req.alias()));

        BigDecimal initial = scale(req.initialDeposit());
        if (initial.signum() > 0) {
            account.deposit(initial);
            txRepository.save(new AccountTransaction(account, TransactionType.DEPOSIT, initial, null, "신규 입금"));
        }
        return AccountDto.Response.from(account);
    }

    public List<AccountDto.Response> list(Long memberId) {
        return accountRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(AccountDto.Response::from).toList();
    }

    public AccountDto.Response get(Long memberId, Long accountId) {
        return AccountDto.Response.from(findOwned(memberId, accountId));
    }

    /** Total of all ACTIVE accounts (JPQL aggregate). */
    public BigDecimal totalBalance(Long memberId) {
        BigDecimal sum = accountRepository.sumActiveBalanceByMember(memberId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Transactional
    public void close(Long memberId, Long accountId) {
        Account account = findOwnedForUpdate(memberId, accountId);
        account.close();
    }

    /** Recipient look-up before a transfer. Only a masked name is exposed. */
    public AccountDto.Lookup lookup(String accountNumber) {
        Account a = accountRepository.findWithMemberByAccountNumber(accountNumber)
                .orElseThrow(() -> BusinessException.notFound("존재하지 않는 계좌입니다."));
        return new AccountDto.Lookup(a.getAccountNumber(), maskName(a.getMember().getName()));
    }

    // ---- money movement --------------------------------------------------

    @Transactional
    public AccountDto.Response deposit(Long memberId, Long accountId, AccountDto.AmountRequest req) {
        BigDecimal amount = scale(req.amount());
        Account account = findOwnedForUpdate(memberId, accountId);
        account.deposit(amount);
        txRepository.save(new AccountTransaction(account, TransactionType.DEPOSIT, amount, null, req.memo()));
        return AccountDto.Response.from(account);
    }

    @Transactional
    public AccountDto.Response withdraw(Long memberId, Long accountId, AccountDto.AmountRequest req) {
        BigDecimal amount = scale(req.amount());
        Account account = findOwnedForUpdate(memberId, accountId);
        account.withdraw(amount);
        txRepository.save(new AccountTransaction(account, TransactionType.WITHDRAW, amount, null, req.memo()));
        return AccountDto.Response.from(account);
    }

    /**
     * Transfer between two accounts in ONE database transaction.
     * Both rows are locked (select ... for update) in a fixed (sorted) order, so two opposite
     * transfers (A->B and B->A) can never dead-lock each other.
     */
    @Transactional
    public void transfer(Long memberId, AccountDto.TransferRequest req) {
        if (req.fromAccountNumber().equals(req.toAccountNumber())) {
            throw BusinessException.badRequest("같은 계좌로는 이체할 수 없습니다.");
        }
        BigDecimal amount = scale(req.amount());

        Map<String, Account> locked = new HashMap<>();
        for (String no : new TreeSet<>(List.of(req.fromAccountNumber(), req.toAccountNumber()))) {
            locked.put(no, accountRepository.findByAccountNumberForUpdate(no)
                    .orElseThrow(() -> BusinessException.notFound("존재하지 않는 계좌입니다. (" + no + ")")));
        }
        Account from = locked.get(req.fromAccountNumber());
        Account to = locked.get(req.toAccountNumber());

        if (!from.getMember().getId().equals(memberId)) {
            throw BusinessException.forbidden("본인 계좌에서만 이체할 수 있습니다.");
        }

        // daily limit (JPQL aggregate)
        BigDecimal usedToday = txRepository.sumAmountSince(from.getId(), TransactionType.TRANSFER_OUT,
                LocalDate.now().atStartOfDay());
        BigDecimal used = usedToday == null ? BigDecimal.ZERO : usedToday;
        if (used.add(amount).compareTo(dailyTransferLimit) > 0) {
            throw BusinessException.badRequest("1일 이체 한도(" + dailyTransferLimit.toPlainString() + "원)를 초과했습니다.");
        }

        from.withdraw(amount);
        to.deposit(amount);
        txRepository.save(new AccountTransaction(from, TransactionType.TRANSFER_OUT, amount,
                to.getAccountNumber(), req.memo()));
        txRepository.save(new AccountTransaction(to, TransactionType.TRANSFER_IN, amount,
                from.getAccountNumber(), req.memo()));

        // push a real-time notification to the receiver, but only after the DB commit succeeded
        Long receiverId = to.getMember().getId();
        Map<String, Object> payload = Map.of("accountNumber", to.getAccountNumber(),
                "fromAccountNumber", from.getAccountNumber(), "amount", amount,
                "balance", to.getBalance(), "memo", req.memo() == null ? "" : req.memo(),
                "at", LocalDateTime.now().toString());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.publish(receiverId, "transfer-in", payload);
            }
        });
    }

    /** CSV of the same QueryDSL search used by the screen (max 10,000 rows). */
    public byte[] exportCsv(Long memberId, TransactionDto.SearchCond cond) {
        List<TransactionDto.Response> rows = txRepository
                .searchByMember(memberId, cond, PageRequest.of(0, 10_000)).getContent();
        StringBuilder sb = new StringBuilder("﻿");   // BOM so Excel opens UTF-8 correctly
        sb.append("일시,계좌번호,구분,상대계좌,금액,거래후잔액,메모\r\n");
        for (TransactionDto.Response r : rows) {
            sb.append(csv(String.valueOf(r.createdAt()))).append(',')
                    .append(csv(r.accountNumber())).append(',')
                    .append(r.type()).append(',')
                    .append(csv(r.counterAccountNumber())).append(',')
                    .append(r.amount().toPlainString()).append(',')
                    .append(r.balanceAfter().toPlainString()).append(',')
                    .append(csv(r.memo())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Quotes the cell and neutralises spreadsheet formulas ("=cmd|..." injection). */
    static String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ---- history / reports -------------------------------------------------

    /** Derived query + paging. */
    public Page<TransactionDto.Response> history(Long memberId, Long accountId, int page, int size) {
        findOwned(memberId, accountId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return txRepository.findByAccountId(accountId, pageable).map(TransactionDto.Response::from);
    }

    /** JPQL constructor expression. */
    public List<TransactionDto.TypeSum> summary(Long memberId, Long accountId) {
        findOwned(memberId, accountId);
        return txRepository.sumByType(accountId);
    }

    /** Native query. */
    public List<TransactionDto.MonthlyView> monthly(Long memberId, Long accountId) {
        findOwned(memberId, accountId);
        return new ArrayList<>(txRepository.monthlyReport(accountId));
    }

    /** QueryDSL dynamic search across all my accounts. */
    public Page<TransactionDto.Response> search(Long memberId, TransactionDto.SearchCond cond, int page, int size) {
        return txRepository.searchByMember(memberId, cond, PageRequest.of(page, size));
    }

    // ---- helpers -----------------------------------------------------------

    private Account findOwned(Long memberId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound("계좌를 찾을 수 없습니다."));
        assertOwner(account, memberId);
        return account;
    }

    private Account findOwnedForUpdate(Long memberId, Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> BusinessException.notFound("계좌를 찾을 수 없습니다."));
        assertOwner(account, memberId);
        return account;
    }

    private static void assertOwner(Account account, Long memberId) {
        // getMember() is a lazy proxy; reading its id does not hit the database
        if (!account.getMember().getId().equals(memberId)) {
            throw BusinessException.notFound("계좌를 찾을 수 없습니다.");   // 404: do not reveal that it exists
        }
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("110-%03d-%06d", RANDOM.nextInt(1000), RANDOM.nextInt(1_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v.setScale(2, RoundingMode.HALF_UP);
    }

    static String maskName(String name) {
        if (name == null || name.length() < 2) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
