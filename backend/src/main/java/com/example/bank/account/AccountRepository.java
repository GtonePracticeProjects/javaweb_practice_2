package com.example.bank.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // ---- (1) derived queries ---------------------------------------------
    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByMemberIdAndStatus(Long memberId, AccountStatus status);

    // ---- (2) JPQL ----------------------------------------------------------

    /** Recipient look-up: account + owner in one select. */
    @Query("select a from Account a join fetch a.member where a.accountNumber = :accountNumber")
    Optional<Account> findWithMemberByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * Pessimistic write lock -> "select ... for update" (row lock until the transaction ends).
     * Used by deposit/withdraw/transfer so concurrent balance changes are serialised.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    /** Aggregate: null when the member has no active account. */
    @Query("select sum(a.balance) from Account a " +
            "where a.member.id = :memberId and a.status = com.example.bank.account.AccountStatus.ACTIVE")
    BigDecimal sumActiveBalanceByMember(@Param("memberId") Long memberId);

    // ---- (3) native SQL ----------------------------------------------------

    /** Bank-wide statistics per account type (interface projection, aliases = getter names). */
    @Query(value = """
            select a.account_type   as accountType,
                   count(*)         as accountCount,
                   sum(a.balance)   as totalBalance
              from account a
             where a.status = 'ACTIVE'
             group by a.account_type
             order by a.account_type
            """, nativeQuery = true)
    List<AccountDto.TypeSummary> summarizeByType();
}
