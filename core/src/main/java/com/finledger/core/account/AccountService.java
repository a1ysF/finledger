package com.finledger.core.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for Accounts. Knows nothing about HTTP or JSON. */
@Service
public class AccountService {

    private static final String MYR = "MYR";

    private final AccountRepository accounts;

    AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * Opens a Customer Account. It always starts at RM0.00, MYR, ACTIVE:
     * money only ever enters through recorded Transfers (PRD story 2).
     */
    @Transactional
    public Account openCustomerAccount(String ownerName) {
        Account account = new Account(
                UUID.randomUUID(),
                ownerName,
                AccountType.CUSTOMER,
                MYR,
                new BigDecimal("0.00"),   // built from a String (rule 1)
                AccountStatus.ACTIVE,
                Instant.now().truncatedTo(ChronoUnit.MICROS));   // TIMESTAMPTZ stores microseconds
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }
}
