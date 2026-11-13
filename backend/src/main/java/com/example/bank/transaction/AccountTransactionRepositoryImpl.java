package com.example.bank.transaction;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.example.bank.account.QAccount.account;
import static com.example.bank.transaction.QAccountTransaction.accountTransaction;

public class AccountTransactionRepositoryImpl implements AccountTransactionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public AccountTransactionRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<TransactionDto.Response> searchByMember(Long memberId, TransactionDto.SearchCond cond,
                                                        Pageable pageable) {
        QAccountTransaction t = accountTransaction;

        // BooleanBuilder: build the WHERE clause step by step
        BooleanBuilder where = new BooleanBuilder(account.member.id.eq(memberId));   // always scope to the owner
        if (cond.accountNumber() != null && !cond.accountNumber().isBlank()) {
            where.and(account.accountNumber.eq(cond.accountNumber()));
        }
        if (cond.type() != null) {
            where.and(t.type.eq(cond.type()));
        }
        if (cond.minAmount() != null) {
            where.and(t.amount.goe(cond.minAmount()));
        }
        if (cond.maxAmount() != null) {
            where.and(t.amount.loe(cond.maxAmount()));
        }
        if (cond.from() != null) {
            where.and(t.createdAt.goe(cond.from().atStartOfDay()));
        }
        if (cond.to() != null) {
            where.and(t.createdAt.lt(cond.to().plusDays(1).atStartOfDay()));
        }
        if (cond.keyword() != null && !cond.keyword().isBlank()) {
            where.and(t.memo.containsIgnoreCase(cond.keyword())
                    .or(t.counterAccountNumber.contains(cond.keyword())));
        }

        // DTO is selected directly -> no entity loading, no lazy proxies
        List<TransactionDto.Response> content = queryFactory
                .select(Projections.constructor(TransactionDto.Response.class,
                        t.id, account.accountNumber, t.type, t.amount, t.balanceAfter,
                        t.counterAccountNumber, t.memo, t.createdAt))
                .from(t)
                .join(t.account, account)
                .where(where)
                .orderBy(t.createdAt.desc(), t.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(t.count())
                .from(t)
                .join(t.account, account)
                .where(where);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = countQuery.fetchOne();
            return total == null ? 0 : total;
        });
    }

    @Override
    public List<TransactionDto.MonthlyStat> monthlyStats(LocalDate from, LocalDate to) {
        QAccountTransaction t = accountTransaction;
        BooleanBuilder where = new BooleanBuilder();
        if (from != null) {
            where.and(t.createdAt.goe(from.atStartOfDay()));
        }
        if (to != null) {
            where.and(t.createdAt.lt(to.plusDays(1).atStartOfDay()));
        }

        return queryFactory
                .select(Projections.constructor(TransactionDto.MonthlyStat.class,
                        t.createdAt.year(), t.createdAt.month(), t.type, t.amount.sum(), t.count()))
                .from(t)
                .where(where)
                .groupBy(t.createdAt.year(), t.createdAt.month(), t.type)
                .orderBy(t.createdAt.year().desc(), t.createdAt.month().desc(), t.type.asc())
                .fetch();
    }
}
