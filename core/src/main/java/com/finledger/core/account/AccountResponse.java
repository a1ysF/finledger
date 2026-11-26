package com.finledger.core.account;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * What a client sees. balance is a String with 2 decimals ("0.00"), so clients
 * never parse money into a floating-point number.
 */
public record AccountResponse(
        UUID id,
        String ownerName,
        AccountType type,
        String currency,
        String balance,
        AccountStatus status,
        Instant createdAt) {

    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getType(),
                account.getCurrency(),
                // Stored as NUMERIC(19,4), shown as MYR with 2 decimals. UNNECESSARY means
                // "fail loudly rather than round": a sub-sen balance would be a bug.
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
