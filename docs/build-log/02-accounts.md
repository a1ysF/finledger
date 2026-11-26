# Build 02: Accounts, first table, first API, first real test

## Summary

Added the first domain feature: client systems can open a Customer Account and read its balance and status through a REST API. It introduces the patterns every later feature reuses: a Flyway migration, a JPA entity behind a repository, a service holding the business rules, record DTOs at the HTTP boundary, validation at the edge, and Problem Details errors. Everything is tested through the HTTP API against a real PostgreSQL 16.

## What was built

- `V1__create_accounts.sql`: `ledger.accounts` with a UUID key, `NUMERIC(19,4)` balance, `CHAR(3)` currency and four CHECK constraints: valid type, valid status, MYR only, and a Customer Account balance that can never go below zero.
- Flyway and Hibernate both pointed at schema `ledger` (Flyway creates it and keeps its history table there).
- `POST /api/v1/accounts` takes only `ownerName` and returns `201 Created` with a `Location` header. The account always opens at RM0.00, MYR, `ACTIVE`, `CUSTOMER`.
- `GET /api/v1/accounts/{id}` returns `200`, `404` for an unknown ID, and `400` for an ID that isn't a UUID.
- Feature package `com.finledger.core.account`: `AccountController` → `AccountService` → `AccountRepository`, with the `Account` entity and the `CreateAccountRequest` / `AccountResponse` records.
- Errors use RFC 7807 Problem Details (Spring's built-in support).

```json
POST /api/v1/accounts  {"ownerName": "Aisyah binti Ahmad"}

201 Created
Location: /api/v1/accounts/6dd06e72-e483-4a18-add8-754d5fed75e9
{"id":"6dd06e72-…","ownerName":"Aisyah binti Ahmad","type":"CUSTOMER","currency":"MYR",
 "balance":"0.00","status":"ACTIVE","createdAt":"2026-11-25T12:22:29.513697Z"}
```

## Key decisions and trade-offs

- **The balance goes out as a JSON string (`"0.00"`), not a number.** Most clients, JavaScript especially, read JSON numbers into binary floating point, which is exactly the rounding error the ledger exists to prevent. A string makes the client parse it with its own decimal type. Trade-off: clients must convert it explicitly. The database keeps 4 decimals; the API shows MYR's 2, and fails loudly (`RoundingMode.UNNECESSARY`) rather than silently rounding a sub-sen value.
- **The request DTO has no `balance`, `type` or `status` field.** A client can't open an account with money in it (mass assignment). Extra JSON fields are ignored, and a test proves it. The test passed on its first run, so it was checked by temporarily adding a `balance` field and watching it fail.
- **Two lines of defence.** Bean Validation rejects bad input at the edge with `400`. CHECK and NOT NULL constraints in Postgres reject anything a bug in our own code lets through. Before validation existed, a test showed blank names being saved and over-long names reaching Postgres as `500`s.
- **Domain exception, HTTP mapping in the controller.** `AccountService` throws `AccountNotFoundException`, which knows nothing about HTTP; the controller maps it to `404`. In Build 08 the few "request never reached a decision" errors will share one base class with a `reasonCode` and one global handler. Business rejections such as `INSUFFICIENT_FUNDS` are deliberately *not* exceptions, because a Rejected Transfer must be stored, and an exception would roll that row back.
- **Timestamps at the database's precision.** Java's clock has nanoseconds and `TIMESTAMPTZ` keeps microseconds (and rounds). At first the create response and a later fetch showed different `createdAt` values for the same account, found during the manual check. A test now pins them equal, and the service truncates to microseconds.
- **The System Account isn't seeded yet.** The table allows `type = 'SYSTEM'`; the row arrives with Deposits in Build 04, where it's first needed.
- **Known inefficiency, left for Build 04.** Because UUIDs are assigned in Java, Spring Data's `save` can't tell an entity is new and runs a `SELECT` before the `INSERT`. It's harmless for accounts but wasteful for ledger lines, so it's scheduled with them (`Persistable<UUID>`).

## Concepts demonstrated

| Concept | Where it lives |
| --- | --- |
| Versioned schema migrations | `core/src/main/resources/db/migration/V1__create_accounts.sql` |
| Exact money storage (`NUMERIC(19,4)`, `BigDecimal` from a String) | the migration; `AccountService.openCustomerAccount` |
| Database constraints as a second line of defence | CHECK constraints in V1 |
| Schema per application | `spring.flyway.schemas`, `hibernate.default_schema` in `application.yml` |
| ORM entity mapping, validated against the real schema | `Account.java`, `ddl-auto: validate` |
| Repository pattern (Spring Data JPA) | `AccountRepository.java` |
| Layered feature package (Controller → Service → Repository) | `com.finledger.core.account` |
| DTOs as a stable API contract; mass-assignment protection | `CreateAccountRequest.java`, `AccountResponse.java` |
| Validation at the edge | `@Valid` in `AccountController`, annotations on `CreateAccountRequest` |
| RFC 7807 Problem Details | `spring.mvc.problemdetails.enabled`; `AccountController.accountNotFound` |
| HTTP semantics: `201` + `Location`, `400` vs `404` | `AccountController.java` |

## How it's tested

`cd core && ./mvnw verify`: 11 tests, 0 failures (Docker must be running).

`AccountApiTest`: 9 tests through the HTTP API with `MockMvcTester` on a Testcontainers PostgreSQL 16. Each one was written to fail first.

- A created account starts at `"0.00"`, MYR, `ACTIVE`, `CUSTOMER`, with a `Location` header; fetching it returns the same values, including the same `createdAt`.
- Blank, empty, null, missing and 101-character owner names → `400` Problem Details (5 cases).
- `balance`, `type` and `status` sent by the client are ignored.
- Unknown ID → `404` Problem Details; malformed ID → `400`.

Tests share one database, so each creates its own account and never assumes the table is empty.

Manual check against the Compose Postgres with curl: create → `201`, fetch → `200`, blank name → `400`, unknown ID → `404`. In `psql` the row shows `balance 0.0000`, and `flyway_schema_history` shows schema creation plus V1.

## Next

Build 03 adds CI: a GitHub Actions workflow that runs `./mvnw verify` (Testcontainers included) on every push, with a status badge in the README.
