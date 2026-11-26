package com.finledger.core.account;

import java.util.UUID;

/**
 * Domain error: no Account has this ID. Says nothing about HTTP; the controller
 * maps it to 404. In Build 08 it will extend a shared base carrying reasonCode
 * ACCOUNT_NOT_FOUND, handled by one global handler.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Account " + id + " does not exist");
    }
}
