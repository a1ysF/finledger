package com.finledger.core.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What a client may send to open an account: the Owner name and nothing else.
 * There is deliberately no balance field, so a client cannot create money.
 * The annotations are checked at the edge (@Valid in the controller), before any code runs.
 */
public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String ownerName) {   // 100 = owner_name VARCHAR(100)
}
