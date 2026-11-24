# FinLedger Core — Product Requirements

Status: living document. Update it when a decision changes; record the reason in `docs/adr/`.

## Problem Statement

Every digital bank has one system that is the single source of truth for "who owns how much": the ledger. Most applications can tolerate a lost update or a duplicate row. A ledger cannot tolerate even one, because every error is someone's money.

A naive implementation (read balance, subtract, save) fails in five specific ways:

| Failure | What happens | Prevention in FinLedger |
| --- | --- | --- |
| Race condition | Two requests both read RM100 and both pay RM80; balance becomes −RM60 | Pessimistic row locks inside one transaction, locked in ID order |
| Rounding error | `0.1 + 0.2` in a `double` is `0.30000000000000004`; sen go missing over millions of transfers | `NUMERIC(19,4)` in Postgres, `BigDecimal` in Java |
| Duplicate Transfer | The client times out, retries, and the customer is charged twice | Idempotency keys |
| Lost audit event | Money moves, then the app crashes before telling the audit system | Transactional outbox |
| Silent drift | A bug corrupts a balance and nobody notices for weeks | Double-entry bookkeeping plus reconciliation |

The project owner is a QA engineer moving into backend engineering, targeting digital banks. The project must prove they understand these failures and can prevent them, with automated tests as evidence.

## Solution

FinLedger Core is an internal platform service, not a customer app. Other bank systems call its REST API to move money. Operations and compliance staff use a small web console to investigate and audit.

- A **core engine** (Spring Boot) moves money atomically using double-entry, pessimistic locking, idempotency keys and simple risk rules.
- Every Transfer writes an event to an **outbox** table in the same transaction. A relay publishes it to **Kafka**.
- A separate **audit consumer** (Spring Boot) stores each event exactly once in an append-only audit log.
- A **reconciliation** check proves the ledger sums to zero and every balance matches its ledger lines.
- A **React console** lets ops and auditors search, freeze accounts, reverse transfers, view the audit trail, and simulate concurrent transfers.
- Everything runs with one `docker compose up`, is tested in GitHub Actions, and is deployed to AWS (EC2 behind API Gateway) with the console on Netlify.

### Personas and roles

| Persona | Role | Uses |
| --- | --- | --- |
| Client system (e.g. a payments backend) | `SERVICE` | REST API, machine-to-machine JWT |
| Operations analyst | `OPS` | Console: investigate, freeze, reverse |
| Compliance officer / auditor | `AUDITOR` | Console, read-only: statements, audit trail |
| Platform engineer | `ADMIN` | Health, metrics, reconciliation |

## User Stories

**Accounts**
1. As a client system, I want to create a Customer Account with an Owner name, so that it can hold money. (Only Customer Accounts can be created; the single System Account is created by a migration.)
2. As a client system, I want new accounts to start at RM0.00 in MYR, so that money only ever enters through recorded Transfers.
3. As a client system, I want to read an account's balance and status, so that I can show it to the customer.
4. As an ops analyst, I want to freeze an account, so that suspected fraud stops immediately.
5. As an ops analyst, I want to unfreeze an account, so that a cleared customer can transact again.
6. As a client system, I want every Transfer into or out of a Frozen account (Deposits, Withdrawals and Payments) to be rejected with `ACCOUNT_FROZEN`, so that freezing is effective. Reversals are the one exception (story 24).

**Money in and out**
7. As a client system, I want to deposit money into an account, so that customers can fund their Accounts.
8. As a client system, I want to withdraw money from an account, so that customers can cash out.
9. As an auditor, I want deposits and withdrawals recorded against the System Account (`SYSTEM`), so that the ledger always balances. The System Account may go negative (its balance is minus all money customers hold), is exempt from `INSUFFICIENT_FUNDS` and both limits, and cannot be frozen.

**Transfers**
10. As a client system, I want to transfer an amount between two accounts, so that customers can pay each other.
11. As a client system, I want amounts with more than 2 decimal places or ≤ 0 rejected, so that invalid money never enters the ledger.
12. As a client system, I want a transfer that would make a customer balance negative rejected with `INSUFFICIENT_FUNDS`.
13. As a client system, I want simultaneous transfers from one account never to overspend it, so that double-spending is impossible.
14. As a client system, I want opposite transfers between two accounts at the same time to complete without deadlock.
15. As a client system, I want to send an `Idempotency-Key` with each transfer, so that retrying after a timeout never moves money twice.
16. As a client system, I want a retried request to return the original result and transfer ID (including a stored rejection), so that I can reconcile my own records. Two requests with the same key arriving at once must also return one result.
17. As a client system, I want reusing a key with a different body (or on a different endpoint) rejected, so that client bugs are caught.
18. As a client system, I want every rejection to carry a stable reason code, so that I can handle it programmatically.

**Risk rules**
19. As a risk owner, I want single transfers above a per-transfer limit (default RM10,000) rejected with `LIMIT_EXCEEDED`.
20. As a risk owner, I want an account's outgoing total for the day limited by the Daily Limit (default RM20,000), rejected with `DAILY_LIMIT_EXCEEDED`. A day is a calendar day in `Asia/Kuala_Lumpur`; the total counts Completed Payments and Withdrawals.
21. As an ops analyst, I want rejected transfers stored with their reason, so that I can explain them to customers.

**Ledger and history**
22. As an auditor, I want each completed transfer to produce exactly two ledger lines that sum to zero.
23. As an auditor, I want ledger lines to be impossible to update or delete, even by the application's database user.
24. As an ops analyst, I want to reverse a Completed Transfer by creating a Reversal linked to the original: once, in full, never for a Rejected Transfer or another Reversal (`ALREADY_REVERSED`, `NOT_REVERSIBLE`). A Reversal ignores freezes and both limits and doesn't count toward the Daily Limit, but is still rejected with `INSUFFICIENT_FUNDS` (ADR 0004).
25. As a client system, I want a paginated account statement with a running balance.

**Audit trail**
26. As an auditor, I want every completed or rejected transfer to appear in an independent audit log.
27. As an auditor, I want transfers to keep working when the audit pipeline is down, and the audit log to catch up afterwards.
28. As an auditor, I want duplicate events never to create duplicate audit rows.
29. As an auditor, I want to look up the audit trail for a transfer or an account.

**Reconciliation**
30. As a platform engineer, I want a check that all ledger lines sum to zero.
31. As a platform engineer, I want a check that each account's balance equals the sum of its ledger lines.
32. As a platform engineer, I want a check that every Transfer, completed or rejected, has an audit record.

**Security**
33. As a platform engineer, I want every endpoint except login and health to require a valid JWT.
34. As a platform engineer, I want each endpoint restricted to the roles in the API table below.
35. As a platform engineer, I want passwords stored only as BCrypt hashes and secrets kept out of Git.

**Console**
36. As an ops analyst, I want to log in and search accounts by owner name or ID.
37. As an ops analyst, I want an account page showing balance, status, statement, and freeze/unfreeze controls.
38. As an ops analyst, I want to search transfers by account and status, and reverse one.
39. As an auditor, I want an audit trail view for a transfer.
40. As a recruiter watching the demo, I want a simulator that fires many parallel transfers at one account, so that I can see double-spending fail safely. The server runs it (`POST /admin/simulations`): each run creates two fresh demo accounts, deposits RM500 into one, and reports the counts and final balance.

## Implementation Decisions

**Architecture**
- Modular monolith (`core`) plus one separate consumer (`audit-consumer`). Not microservices: splitting accounts and transfers would turn one local transaction into a distributed one (ADR 0001).
- Money path is synchronous and transactional (core → Postgres). Audit path is asynchronous (outbox → Kafka → consumer), so a broken audit pipeline never blocks Transfers (ADR 0002).
- One Postgres instance, two schemas: `ledger` (owned by core) and `audit` (owned by the consumer). Each app's DB user can only write its own schema.
- Kafka runs as a single node in KRaft mode inside Docker Compose. Managed Kafka (MSK) is out of budget; the README notes it is the production choice.

```
Client systems ─┐                      ┌─> Kafka ──> Audit consumer ──> audit.audit_log
Ops console ────┴─> API Gateway ─> Core ┤
 (Netlify)          (AWS)        (EC2)  └─> Postgres ledger schema (accounts, transfers,
                                                    ledger_lines, outbox_events)
Stretch: Audit consumer ─> S3 (Object Lock) · nightly Lambda ─> GET /admin/reconciliation ─> S3
```

**Life of one transfer**
1. `POST /api/v1/transfers` with JWT and `Idempotency-Key`.
2. Spring Security checks token and role; controller validates the DTO.
3. Service looks up the idempotency key for this client; if found, returns the stored result.
4. In one transaction: lock both accounts in ascending ID order (ADR 0005), run risk rules, update balances, insert transfer, two ledger lines and one outbox event. A rejection inserts the transfer (status `REJECTED`) and its outbox event, and changes no balance.
5. Commit; respond `201 Created` (or `422` with reason; `200` on replay). A lock timeout or database failure rolls everything back and returns `503 TRY_AGAIN`; the client retries with the same key.
6. Relay publishes unpublished outbox rows to topic `transfer-events`, then marks them published.
7. Audit consumer inserts each event by `event_id`; duplicates are ignored.

**Data model (ledger schema)**

| Table | Key columns |
| --- | --- |
| `accounts` | `id` UUID PK, `owner_name`, `type` (`CUSTOMER`/`SYSTEM`), `currency` CHAR(3), `balance` NUMERIC(19,4), `status` (`ACTIVE`/`FROZEN`), `created_at` |
| `transfers` | `id` UUID PK, `kind` (`DEPOSIT`/`WITHDRAWAL`/`PAYMENT`/`REVERSAL`), `client_id`, `idempotency_key` (UNIQUE with `client_id`), `request_hash` (includes `kind`), `from_account_id`, `to_account_id`, `amount` NUMERIC(19,4), `status` (`COMPLETED`/`REJECTED`), `reason_code`, `reversal_of_id`, `created_at` |
| `ledger_lines` | `id`, `transfer_id`, `account_id`, `amount` (negative = money out of the Account), `balance_after`, `created_at` |
| `outbox_events` | `id` UUID, `event_type` (`TransferCompleted`/`TransferRejected`; payload carries `kind` and both account IDs), `payload` JSONB, `created_at`, `published_at` |
| `app_users` | `username`, `password_hash`, `role` |

Audit schema: `audit_log` (`event_id` UNIQUE, `event_type`, `payload` JSONB, `received_at`).

`accounts.balance` is a cached copy for fast reads; `ledger_lines` is the truth. Reconciliation checks they agree. `transfers` and `ledger_lines` are append-only, enforced for the application's database user; in `accounts` only `balance` and `status` change, and in `outbox_events` only `published_at`.

**API** (JSON, prefix `/api/v1`, documented with Swagger UI)

| Method + path | Purpose | Roles |
| --- | --- | --- |
| `POST /auth/login` | Username + password → JWT | public |
| `POST /accounts` | Create a Customer Account (`ownerName` only) | SERVICE, ADMIN |
| `GET /accounts/{id}` | Balance and status | SERVICE, OPS, AUDITOR |
| `GET /accounts?owner=` | Search accounts | OPS, AUDITOR |
| `GET /accounts/{id}/statement` | Paginated ledger lines | SERVICE, OPS, AUDITOR |
| `POST /accounts/{id}/freeze`, `/unfreeze` | Change status | OPS |
| `POST /deposits`, `POST /withdrawals` | Money in / out (needs `Idempotency-Key`) | SERVICE |
| `POST /transfers` | Move money (needs `Idempotency-Key`) | SERVICE |
| `GET /transfers?accountId=&status=` | Search transfers | OPS, AUDITOR |
| `POST /transfers/{id}/reverse` | Create a Reversal (needs `Idempotency-Key`) | OPS |
| `GET /audit?transferId=&accountId=` | Audit trail (served by audit-consumer) | OPS, AUDITOR |
| `GET /admin/reconciliation` | Run balance checks | ADMIN |
| `POST /admin/simulations` | Run N parallel Payments between two fresh demo accounts | ADMIN |

**Other decisions**
- Java 21, Spring Boot 4, Maven. (Boot 4 replaced 3.5, whose open-source support ended 2026-06-30.) UUID primary keys generated in Java.
- Isolation level: Postgres default `READ COMMITTED`, made safe by explicit row locks.
- Lock timeout set (e.g. 5 s) so a stuck transaction fails instead of hanging.
- Errors follow RFC 7807 Problem Details with a `reasonCode` field.
- **Idempotency (ADR 0003):** keys are unique per client (the `SERVICE` client, or the ops user for Reversals), shared across all money endpoints, and never expire. A Rejected Transfer uses up its key. Requests that never reached a decision store nothing and leave the key free: validation `400`, `404 ACCOUNT_NOT_FOUND`, `400 SAME_ACCOUNT`, `503 TRY_AGAIN`.
- Per-Transfer Limit and Daily Limit are global settings (environment variables); no per-account limits in v1.
- On startup, core creates one user per role from environment variables if missing; no user-management API in v1.
- Console: React + TypeScript (Vite), hosted on Netlify. Production deploys only from `main` (each production deploy uses Netlify credits; previews are free).
- Cost target $0: local Docker, AWS Free Tier credits with a budget alert, Netlify Free plan.

## Testing Decisions

A good test checks behaviour through a public interface (HTTP API or service method), not private details. Money rules are tested against a **real Postgres** via Testcontainers, never an in-memory fake, because locking behaviour differs.

| Level | Tools | What it proves |
| --- | --- | --- |
| Unit | JUnit 6, Mockito | Money validation, risk rules, lock-ordering helper, request hashing |
| Integration | Testcontainers (Postgres, Kafka), Spring Boot Test | Repositories, transactions, outbox, consumer, security |
| Concurrency | `ExecutorService` + `CountDownLatch` against Testcontainers | No double-spend, no deadlock |
| API | MockMvc or RestAssured | Status codes, reason codes, roles |
| Console | Vitest, React Testing Library | Key screens render and call the API |

**Acceptance tests (v1 is done when all pass in CI):**
1. **Double-spend:** RM500 balance, 100 parallel transfers of RM10 → exactly 50 complete, 50 `INSUFFICIENT_FUNDS`, final balance RM0.00.
2. **Deadlock:** 50 A→B and 50 B→A in parallel → all finish, no deadlock errors, A + B unchanged.
3. **Idempotency:** same request sent 5 times in parallel with one key → money moves once, same transfer ID each time.
4. **Rounding:** RM0.10 + RM0.20 + RM0.30 → exactly RM0.60; 3-decimal amounts rejected.
5. **Outbox:** with Kafka stopped, Transfers complete; after restart, every event is audited.
6. **Duplicate event:** same event delivered twice → one audit row.
7. **Invariant:** after any test run, all ledger lines sum to 0 and every balance equals the sum of its lines.

## Out of Scope

Multi-currency and FX · interest and fees · real payment rails (DuitNow, FPX, cards) · KYC and onboarding · customer mobile app · microservices, Kubernetes, service mesh · WildFly/JBoss deployment (target employers deploy containers).

## Stretch (after v1)

S3 audit archive with Object Lock · nightly reconciliation Lambda · least-privilege IAM role per component, policies in `infra/iam/` · infrastructure as code (Terraform or AWS CDK) · k6 load test with published numbers · optional: move the console from Netlify to S3 + CloudFront.
