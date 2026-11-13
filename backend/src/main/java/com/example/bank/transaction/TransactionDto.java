package com.example.bank.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TransactionDto {

    private TransactionDto() {
    }

    public record Response(Long id, String accountNumber, TransactionType type, BigDecimal amount,
                           BigDecimal balanceAfter, String counterAccountNumber, String memo,
                           LocalDateTime createdAt) {
        public static Response from(AccountTransaction t) {
            return new Response(t.getId(), t.getAccount().getAccountNumber(), t.getType(), t.getAmount(),
                    t.getBalanceAfter(), t.getCounterAccountNumber(), t.getMemo(), t.getCreatedAt());
        }
    }

    /** Dynamic search condition for QueryDSL (every field optional). */
    public record SearchCond(String accountNumber, TransactionType type,
                             BigDecimal minAmount, BigDecimal maxAmount,
                             LocalDate from, LocalDate to, String keyword) {
    }

    /** JPQL constructor expression: totals per transaction type of one account. */
    public record TypeSum(TransactionType type, Long count, BigDecimal total) {
    }

    /** QueryDSL group-by result (year/month/type). */
    public record MonthlyStat(Integer year, Integer month, TransactionType type, BigDecimal total, Long count) {
    }

    /** Native-query interface projection (monthly report of one account). */
    public interface MonthlyView {
        Integer getYr();

        Integer getMon();

        String getTxType();

        BigDecimal getTotalAmount();

        Long getCnt();
    }
}
