package com.example.bank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/** QueryDSL fragment (see AccountTransactionRepositoryImpl). */
public interface AccountTransactionRepositoryCustom {

    /** Dynamic search over all accounts that belong to {@code memberId}. */
    Page<TransactionDto.Response> searchByMember(Long memberId, TransactionDto.SearchCond cond, Pageable pageable);

    /** Bank-wide monthly totals for the admin dashboard. */
    List<TransactionDto.MonthlyStat> monthlyStats(LocalDate from, LocalDate to);
}
