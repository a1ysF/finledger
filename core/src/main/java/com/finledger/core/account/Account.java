package com.finledger.core.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity: Java's view of one row in ledger.accounts.
 * Internal only. The API never returns it; it returns AccountResponse (rule 9).
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountType type;

    @JdbcTypeCode(SqlTypes.CHAR)   // column is CHAR(3), not VARCHAR
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA, which builds the object first and then fills the fields from the row. */
    protected Account() {
    }

    Account(UUID id, String ownerName, AccountType type, String currency,
            BigDecimal balance, AccountStatus status, Instant createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public AccountType getType() { return type; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
