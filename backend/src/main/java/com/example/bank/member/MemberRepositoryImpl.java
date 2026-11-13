package com.example.bank.member;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.bank.account.QAccount.account;
import static com.example.bank.member.QMember.member;

/**
 * QueryDSL implementation. Spring Data picks it up by the "Impl" suffix.
 * Each condition method returns null when its argument is empty -> QueryDSL skips null predicates,
 * which makes dynamic queries trivial.
 */
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<MemberDto.Summary> search(MemberDto.SearchCond cond, Pageable pageable) {
        Predicate[] where = {
                loginIdContains(cond.loginId()),
                nameContains(cond.name()),
                statusEq(cond.status()),
                createdGoe(cond.createdFrom()),
                createdLt(cond.createdTo())
        };
        BooleanExpression having = totalBalanceGoe(cond.minTotalBalance());

        // member LEFT JOIN account, aggregated per member
        List<MemberDto.Summary> content = queryFactory
                .select(Projections.constructor(MemberDto.Summary.class,
                        member.id, member.loginId, member.name, member.email, member.status,
                        member.createdAt,
                        account.count(),
                        account.balance.sum().coalesce(BigDecimal.ZERO)))
                .from(member)
                .leftJoin(member.accounts, account)
                .where(where)
                .groupBy(member.id, member.loginId, member.name, member.email, member.status, member.createdAt)
                .having(having)
                .orderBy(member.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count query is lazily executed only when the page is not obviously complete
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            if (having == null) {
                Long total = queryFactory.select(member.count()).from(member).where(where).fetchOne();
                return total == null ? 0 : total;
            }
            // with HAVING we have to count the grouped rows
            return queryFactory.select(member.id)
                    .from(member).leftJoin(member.accounts, account)
                    .where(where).groupBy(member.id).having(having)
                    .fetch().size();
        });
    }

    private BooleanExpression loginIdContains(String v) {
        return hasText(v) ? member.loginId.containsIgnoreCase(v) : null;
    }

    private BooleanExpression nameContains(String v) {
        return hasText(v) ? member.name.contains(v) : null;
    }

    private BooleanExpression statusEq(MemberStatus v) {
        return v != null ? member.status.eq(v) : null;
    }

    private BooleanExpression createdGoe(LocalDate v) {
        return v != null ? member.createdAt.goe(v.atStartOfDay()) : null;
    }

    private BooleanExpression createdLt(LocalDate v) {
        return v != null ? member.createdAt.lt(v.plusDays(1).atStartOfDay()) : null;
    }

    private BooleanExpression totalBalanceGoe(BigDecimal v) {
        return v != null ? account.balance.sum().goe(v) : null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
