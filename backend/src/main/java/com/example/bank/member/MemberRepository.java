package com.example.bank.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Query styles used in this repository
 *  (1) Spring Data derived queries     : findByLoginId, existsBy...
 *  (2) JPQL  (@Query)                  : fetch join, constructor expression, bulk update
 *  (3) Native SQL (@Query nativeQuery) : findByEmailDomainNative
 *  (4) QueryDSL (MemberRepositoryCustom / MemberRepositoryImpl) : dynamic admin search
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    // ---- (1) derived queries ---------------------------------------------
    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    // ---- (2) JPQL ----------------------------------------------------------

    /** fetch join: member + accounts in ONE select (avoids N+1). */
    @Query("select distinct m from Member m left join fetch m.accounts where m.id = :id")
    Optional<Member> findWithAccountsById(@Param("id") Long id);

    /** Constructor expression -> DTO straight from JPQL. */
    @Query("select new com.example.bank.member.MemberDto$StatusCount(m.status, count(m)) " +
            "from Member m group by m.status order by m.status")
    List<MemberDto.StatusCount> countByStatus();

    /**
     * Bulk UPDATE (no entity loading; bypasses dirty checking and auditing, so updatedAt is set by hand).
     * clearAutomatically: the persistence context would otherwise hold stale entities.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Member m set m.status = :to, m.updatedAt = current_timestamp " +
            "where m.status = :from and m.role = com.example.bank.member.Role.USER " +
            "and (m.lastLoginAt < :threshold or m.lastLoginAt is null)")
    int bulkChangeStatusByLastLogin(@Param("from") MemberStatus from,
                                    @Param("to") MemberStatus to,
                                    @Param("threshold") LocalDateTime threshold);

    // ---- (3) native SQL ----------------------------------------------------

    /** Plain SQL returning managed entities. */
    @Query(value = "select * from member where email like concat('%@', :domain) order by id",
            nativeQuery = true)
    List<Member> findByEmailDomainNative(@Param("domain") String domain);
}
