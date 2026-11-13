package com.example.bank.account;

import com.example.bank.common.BaseTimeEntity;
import com.example.bank.common.BusinessException;
import com.example.bank.member.Member;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Bank account. {@code @Version} gives optimistic locking as a second safety net
 * (the money-moving paths additionally use PESSIMISTIC_WRITE, see AccountRepository).
 */
@Entity
@Table(name = "account", indexes = {
        @Index(name = "idx_account_member", columnList = "member_id")
})
@DynamicUpdate
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;

    @Column(length = 50)
    private String alias;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Version
    private Long version;

    protected Account() {
    }

    public Account(String accountNumber, Member member, AccountType accountType, String alias) {
        this.accountNumber = accountNumber;
        this.member = member;
        this.accountType = accountType;
        this.alias = alias;
    }

    // ---- domain behaviour -------------------------------------------------

    public void deposit(BigDecimal amount) {
        assertActive();
        assertPositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        assertActive();
        assertPositive(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw BusinessException.badRequest("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void close() {
        if (this.balance.signum() != 0) {
            throw BusinessException.badRequest("잔액이 남아 있는 계좌는 해지할 수 없습니다.");
        }
        this.status = AccountStatus.CLOSED;
    }

    public void changeStatus(AccountStatus status) {
        this.status = status;
    }

    private void assertActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw BusinessException.badRequest("거래 가능한 계좌가 아닙니다. (" + accountNumber + ")");
        }
    }

    private static void assertPositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw BusinessException.badRequest("금액은 0보다 커야 합니다.");
        }
    }

    // ---- getters ----------------------------------------------------------

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Member getMember() { return member; }
    public AccountType getAccountType() { return accountType; }
    public String getAlias() { return alias; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}
