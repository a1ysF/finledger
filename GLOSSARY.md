# FinLedger Core

The ledger of a digital bank: the single source of truth for who owns how much money, and the record of every movement that changed it.

## Accounts

**Account**:
A container that holds an amount of Malaysian Ringgit (MYR) on behalf of an owner.
_Avoid_: Wallet, balance holder

**Customer Account**:
An Account belonging to a bank customer; its balance can never go below zero.
_Avoid_: User account, client account

**System Account**:
The single clearing Account through which all money enters and leaves the bank's ledger; its balance is the negative of all money held by customers.
_Avoid_: Clearing account, bank account, house account

**Owner**:
The person or business named on a Customer Account.
_Avoid_: Customer name, holder

**Frozen**:
The state of a Customer Account that blocks every Transfer into or out of it.
_Avoid_: Locked, blocked, suspended

## Money movement

**Transfer**:
One attempt to move an amount of money from one Account to another, recorded once and never changed.
_Avoid_: Transaction, movement, payment (as the umbrella term)

**Deposit**:
A Transfer from the System Account into a Customer Account.
_Avoid_: Top-up, credit, cash-in

**Withdrawal**:
A Transfer from a Customer Account to the System Account.
_Avoid_: Cash-out, debit, payout

**Payment**:
A Transfer between two Customer Accounts.
_Avoid_: P2P, internal transfer

**Reversal**:
A new Transfer that undoes a Completed Transfer, once and in full, by moving the same amount in the opposite direction, linked to the original.
_Avoid_: Refund, rollback, correction, void

**Completed Transfer**:
A Transfer that moved money.
_Avoid_: Successful, posted, settled

**Rejected Transfer**:
A Transfer refused by a business rule, recorded with its Reason Code, that moved no money.
_Avoid_: Failed, declined, invalid request

**Reason Code**:
The stable name for why a Transfer was rejected, such as `INSUFFICIENT_FUNDS`.
_Avoid_: Error code, failure reason

**Idempotency Key**:
A client-chosen value that identifies one Transfer attempt from one client system, so a retry returns the original result instead of moving money again.
_Avoid_: Request ID, dedupe key

## Risk rules

**Per-Transfer Limit**:
The largest amount a single Transfer out of a Customer Account may move.
_Avoid_: Max amount, cap

**Daily Limit**:
The largest total a Customer Account may send out in Completed Transfers within one calendar day, Malaysian time.
_Avoid_: Velocity limit, daily cap

## Ledger

**Ledger Line**:
One signed amount recorded against one Account by a Completed Transfer; every Completed Transfer has exactly two, summing to zero.
_Avoid_: Entry, posting, journal line
