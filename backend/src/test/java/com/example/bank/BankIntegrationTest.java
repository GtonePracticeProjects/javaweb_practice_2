package com.example.bank;

import com.example.bank.account.*;
import com.example.bank.common.BusinessException;
import com.example.bank.member.*;
import com.example.bank.transaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises every query style against H2 (MariaDB mode). */
@SpringBootTest
@ActiveProfiles("test")
class BankIntegrationTest {

    @Autowired MemberRepository memberRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountTransactionRepository txRepository;
    @Autowired AccountService accountService;
    @Autowired com.example.bank.auth.AuthService authService;
    @Autowired MemberService memberService;
    @Autowired com.example.bank.admin.StatisticsService statisticsService;

    Member alice;
    Member bob;
    AccountDto.Response aliceAcc;
    AccountDto.Response bobAcc;

    @BeforeEach
    void setUp() {
        txRepository.deleteAll();
        accountRepository.deleteAll();
        memberRepository.deleteAll();

        alice = authService.signup(new MemberDto.SignupRequest("alice", "password1", "앨리스김", "alice@bank.test", "010-1111-1111"));
        bob = authService.signup(new MemberDto.SignupRequest("bobby", "password2", "밥", "bob@other.test", "010-2222-2222"));
        aliceAcc = accountService.open(alice.getId(), new AccountDto.OpenRequest(AccountType.CHECKING, "main", new BigDecimal("100000")));
        bobAcc = accountService.open(bob.getId(), new AccountDto.OpenRequest(AccountType.SAVINGS, null, new BigDecimal("5000")));
    }

    @Test
    void derivedAndJpaQueries() {
        assertThat(memberRepository.findByLoginId("alice")).isPresent();
        assertThat(memberRepository.existsByEmail("bob@other.test")).isTrue();
        assertThat(memberRepository.findWithAccountsById(alice.getId()).orElseThrow().getAccounts()).hasSize(1);
        assertThat(memberRepository.findByEmailDomainNative("other.test")).extracting(Member::getLoginId).containsExactly("bobby");
        assertThat(accountService.totalBalance(alice.getId())).isEqualByComparingTo("100000");
        assertThat(accountService.lookup(aliceAcc.accountNumber()).ownerName()).isEqualTo("앨**김");
    }

    @Test
    void transferMovesMoneyAndWritesTwoLedgerRows() {
        accountService.transfer(alice.getId(), new AccountDto.TransferRequest(
                aliceAcc.accountNumber(), bobAcc.accountNumber(), new BigDecimal("30000"), "lunch"));

        assertThat(accountService.get(alice.getId(), aliceAcc.id()).balance()).isEqualByComparingTo("70000");
        assertThat(accountService.get(bob.getId(), bobAcc.id()).balance()).isEqualByComparingTo("35000");

        // JPQL constructor expression
        List<TransactionDto.TypeSum> sums = accountService.summary(alice.getId(), aliceAcc.id());
        assertThat(sums).extracting(TransactionDto.TypeSum::type)
                .containsExactlyInAnyOrder(TransactionType.DEPOSIT, TransactionType.TRANSFER_OUT);

        // derived query + paging
        assertThat(accountService.history(alice.getId(), aliceAcc.id(), 0, 10).getTotalElements()).isEqualTo(2);

        // native query
        List<TransactionDto.MonthlyView> monthly = accountService.monthly(alice.getId(), aliceAcc.id());
        assertThat(monthly).hasSize(2);
        assertThat(monthly.get(0).getTotalAmount()).isNotNull();
        assertThat(monthly.get(0).getYr()).isEqualTo(LocalDateTime.now().getYear());
    }

    @Test
    void transferRules() {
        assertThatThrownBy(() -> accountService.transfer(alice.getId(), new AccountDto.TransferRequest(
                aliceAcc.accountNumber(), bobAcc.accountNumber(), new BigDecimal("999999"), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("잔액");
        assertThatThrownBy(() -> accountService.transfer(alice.getId(), new AccountDto.TransferRequest(
                bobAcc.accountNumber(), aliceAcc.accountNumber(), new BigDecimal("1"), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("본인");
        assertThatThrownBy(() -> accountService.get(alice.getId(), bobAcc.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void queryDslTransactionSearch() {
        accountService.withdraw(alice.getId(), aliceAcc.id(), new AccountDto.AmountRequest(new BigDecimal("1000"), "coffee"));
        accountService.withdraw(alice.getId(), aliceAcc.id(), new AccountDto.AmountRequest(new BigDecimal("50000"), "rent"));

        Page<TransactionDto.Response> all = txRepository.searchByMember(alice.getId(),
                new TransactionDto.SearchCond(null, null, null, null, null, null, null), PageRequest.of(0, 10));
        assertThat(all.getTotalElements()).isEqualTo(3);

        Page<TransactionDto.Response> filtered = txRepository.searchByMember(alice.getId(),
                new TransactionDto.SearchCond(null, TransactionType.WITHDRAW, new BigDecimal("10000"), null, null, null, "RENT"),
                PageRequest.of(0, 10));
        assertThat(filtered.getContent()).hasSize(1);
        assertThat(filtered.getContent().get(0).memo()).isEqualTo("rent");

        // other member's rows are never visible
        assertThat(txRepository.searchByMember(bob.getId(),
                new TransactionDto.SearchCond(aliceAcc.accountNumber(), null, null, null, null, null, null),
                PageRequest.of(0, 10)).getTotalElements()).isZero();

        assertThat(statisticsService.monthlyTransactions(null, null)).isNotEmpty();
    }

    @Test
    void queryDslMemberSearchAndBulkUpdate() {
        Page<MemberDto.Summary> page = memberRepository.search(
                new MemberDto.SearchCond("ali", null, null, null, null, null), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).accountCount()).isEqualTo(1L);
        assertThat(page.getContent().get(0).totalBalance()).isEqualByComparingTo("100000");

        // HAVING on aggregated balance
        Page<MemberDto.Summary> rich = memberRepository.search(
                new MemberDto.SearchCond(null, null, MemberStatus.ACTIVE, new BigDecimal("50000"), null, null), PageRequest.of(0, 10));
        assertThat(rich.getContent()).extracting(MemberDto.Summary::loginId).containsExactly("alice");
        assertThat(rich.getTotalElements()).isEqualTo(1);

        // bulk update: nobody has logged in -> lastLoginAt is null -> both become DORMANT
        assertThat(memberService.markDormant()).isEqualTo(2);
        assertThat(memberService.countByStatus()).extracting(MemberDto.StatusCount::status).containsExactly(MemberStatus.DORMANT);
    }

    @Test
    void loginFailureLocksAccount() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new MemberDto.LoginRequest("alice", "wrong-password")))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(memberRepository.findByLoginId("alice").orElseThrow().getStatus()).isEqualTo(MemberStatus.LOCKED);
    }

    @Autowired com.example.bank.auth.JwtProvider jwtProvider;

    @Test
    void jwtRoundTripAndTamperDetection() {
        String token = jwtProvider.issue(alice);
        assertThat(jwtProvider.parse(token).memberId()).isEqualTo(alice.getId());
        assertThat(jwtProvider.parse(token).role()).isEqualTo(Role.USER);
        assertThatThrownBy(() -> jwtProvider.parse(token + "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> jwtProvider.parse("not-a-token")).isInstanceOf(BusinessException.class);
    }

    @Test
    void csvExportEscapesFormulasAndQuotes() {
        accountService.withdraw(alice.getId(), aliceAcc.id(), new AccountDto.AmountRequest(new BigDecimal("10"), "=SUM(A1)"));
        accountService.withdraw(alice.getId(), aliceAcc.id(), new AccountDto.AmountRequest(new BigDecimal("10"), "say \"hi\", ok"));
        String csv = new String(accountService.exportCsv(alice.getId(),
                new TransactionDto.SearchCond(null, null, null, null, null, null, null)), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿일시,");
        assertThat(csv).contains("\"'=SUM(A1)\"").contains("\"say \"\"hi\"\", ok\"");
        assertThat(csv.lines().count()).isEqualTo(4);   // header + deposit + 2 withdrawals
    }

    @Test
    void nativeStatistics() {
        List<AccountDto.TypeSummary> stats = statisticsService.accountsByType();
        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).getAccountType()).isEqualTo("CHECKING");
        assertThat(stats.get(0).getTotalBalance()).isEqualByComparingTo("100000");
    }
}
