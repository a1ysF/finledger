# Modular monolith plus a separate audit consumer

`core` is one Spring Boot application holding accounts, Transfers, the ledger and the outbox; only the audit consumer is a separate application. Splitting accounts and Transfers into services would turn one local database transaction into a distributed one, which is exactly where money gets lost or duplicated. The audit consumer is separate because it must stay independent of the system it audits and must never slow down or block a Transfer.

## Considered Options

- **Microservices** (accounts, transfers, ledger as separate services): rejected; needs sagas or two-phase commit to keep a Transfer atomic.
- **Everything in one app, audit included**: rejected; an auditor should not trust an audit log written by the same process it audits, and a slow audit write would delay Transfers.
