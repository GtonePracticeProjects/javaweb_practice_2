package com.example.bank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountTransactionRepository
        extends JpaRepository<AccountTransaction, Long>, AccountTransactionRepositoryCustom {

    // ---- (1) derived query + paging + entity graph -------------------------
    @EntityGraph(attributePaths = "account")
    Page<AccountTransaction> findByAccountId(Long accountId, Pageable pageable);

    // ---- (2) JPQL ----------------------------------------------------------

    /** Constructor expression + aggregate. */
    @Query("select new com.example.bank.transaction.TransactionDto$TypeSum(t.type, count(t), sum(t.amount)) " +
            "from AccountTransaction t where t.account.id = :accountId group by t.type order by t.type")
    List<TransactionDto.TypeSum> sumByType(@Param("accountId") Long accountId);

    /** Used for the daily transfer limit. Returns null when there is no row. */
    @Query("select sum(t.amount) from AccountTransaction t " +
            "where t.account.id = :accountId and t.type = :type and t.createdAt >= :since")
    BigDecimal sumAmountSince(@Param("accountId") Long accountId,
                              @Param("type") TransactionType type,
                              @Param("since") LocalDateTime since);

    // ---- (3) native SQL ----------------------------------------------------

    /** Monthly report of one account (MariaDB/H2 portable functions: year(), month()). */
    @Query(value = """
            select year(t.created_at)  as yr,
                   month(t.created_at) as mon,
                   t.tx_type           as txType,
                   sum(t.amount)       as totalAmount,
                   count(*)            as cnt
              from account_transaction t
             where t.account_id = :accountId
             group by year(t.created_at), month(t.created_at), t.tx_type
             order by yr desc, mon desc, txType
            """, nativeQuery = true)
    List<TransactionDto.MonthlyView> monthlyReport(@Param("accountId") Long accountId);
}
