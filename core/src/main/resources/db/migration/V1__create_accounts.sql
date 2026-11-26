-- Accounts: who holds how much MYR. Runs in schema "ledger" (set in application.yml).
-- balance is a cached copy; from Build 04 the ledger lines are the truth.

CREATE TABLE accounts (
    id          UUID          PRIMARY KEY,                 -- generated in Java
    owner_name  VARCHAR(100)  NOT NULL,
    type        VARCHAR(10)   NOT NULL,
    currency    CHAR(3)       NOT NULL,
    balance     NUMERIC(19,4) NOT NULL,                    -- never DOUBLE PRECISION (rule 1)
    status      VARCHAR(10)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT accounts_type_valid      CHECK (type IN ('CUSTOMER', 'SYSTEM')),
    CONSTRAINT accounts_status_valid    CHECK (status IN ('ACTIVE', 'FROZEN')),
    CONSTRAINT accounts_currency_myr    CHECK (currency = 'MYR'),
    -- A Customer Account can never go below zero; only the System Account may.
    CONSTRAINT accounts_customer_not_negative CHECK (type = 'SYSTEM' OR balance >= 0)
);
