package com.example.bank.transaction;

import com.example.bank.account.Account;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable ledger row. (Table is not named "transaction" to avoid the SQL keyword.) */
@Entity
@Table(name = "account_transaction", indexes = {
        @Index(name = "idx_tx_account_created", columnList = "account_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 15)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "counter_account_number", length = 20)
    private String counterAccountNumber;

    @Column(length = 100)
    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AccountTransaction() {
    }

    public AccountTransaction(Account account, TransactionType type, BigDecimal amount,
                              String counterAccountNumber, String memo) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = account.getBalance();   // call AFTER balance has been changed
        this.counterAccountNumber = counterAccountNumber;
        this.memo = memo;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getCounterAccountNumber() { return counterAccountNumber; }
    public String getMemo() { return memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
