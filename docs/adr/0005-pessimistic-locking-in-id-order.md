# Pessimistic row locks in ascending ID order, at READ COMMITTED

A Transfer locks both Accounts with `SELECT … FOR UPDATE` in ascending ID order, inside a `READ COMMITTED` transaction with a 5-second lock timeout. Locking prevents two Transfers from reading the same balance and both spending it; always locking the lower ID first means two opposite Transfers (A→B and B→A) wait for each other instead of deadlocking.

## Considered Options

- **Optimistic locking** (version column, retry on conflict): rejected; a hot Account (a popular merchant, the System Account) would make most Transfers conflict and retry, and every caller would need retry logic.
- **`SERIALIZABLE` isolation**: rejected; Postgres would abort conflicting Transfers with serialization errors that must be retried, and the locking would be implicit rather than visible in the code.
- **Locking sender first**: rejected; A→B and B→A at the same time each hold one lock and wait for the other, which is a deadlock.
