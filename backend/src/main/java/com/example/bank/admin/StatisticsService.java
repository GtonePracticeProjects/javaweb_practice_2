package com.example.bank.admin;

import com.example.bank.account.AccountDto;
import com.example.bank.account.AccountRepository;
import com.example.bank.member.MemberDto;
import com.example.bank.member.MemberRepository;
import com.example.bank.transaction.AccountTransactionRepository;
import com.example.bank.transaction.TransactionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Admin dashboard: one statistic per query style. */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final AccountTransactionRepository txRepository;

    public StatisticsService(AccountRepository accountRepository, MemberRepository memberRepository,
                             AccountTransactionRepository txRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.txRepository = txRepository;
    }

    /** native SQL */
    public List<AccountDto.TypeSummary> accountsByType() {
        return accountRepository.summarizeByType();
    }

    /** JPQL (constructor expression) */
    public List<MemberDto.StatusCount> membersByStatus() {
        return memberRepository.countByStatus();
    }

    /** QueryDSL (group by year/month/type) */
    public List<TransactionDto.MonthlyStat> monthlyTransactions(LocalDate from, LocalDate to) {
        return txRepository.monthlyStats(from, to);
    }
}
