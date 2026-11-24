# FinLedger Builds

Small builds of about 1–2 hours each. You start each one when you're ready; there is no deadline. Each build ends with something that runs and a test that proves it.

## Tracker

Update the status after each build: `Not started`, `In progress`, `Done`. Put the date and anything worth remembering in Notes.

| # | Build | Phase | Status | Done on | Notes |
| --- | --- | --- | --- | --- | --- |
| 00 | Repo and workflow setup | Foundation | Done | 2026-11-23 | Docs moved into repo; skills set up (local-markdown issues). `/grill-with-docs` on PRD → GLOSSARY.md, 5 ADRs, PRD updated. Two-log build flow added. Quiz: CLAUDE.md vs PRD.md mostly right (missed "loaded every session"); fresh session half right (stale context, not "full"). |
| 01 | Spring Boot skeleton + Postgres in Docker | Foundation | Done | 2026-11-24 | Spring Boot 4.2.0 (3.5 open-source support ended 2026-06-30; CLAUDE.md and PRD updated). One git-ignored `infra/.env` feeds Compose and Spring (`spring.config.import`). Health test red→green on `components.db`. Quiz: image vs container (credentials and data aren't in the image), DB URL source (tests use Testcontainers, not Compose); both right on retry; Postgres-down right. |
| 02 | Accounts: first table, first API, first real test | Foundation | Not started | | |
| 03 | CI: tests run on every push | Foundation | Not started | | |
| 04 | Deposits and withdrawals with double-entry | Money movement | Not started | | |
| 05 | Transfers: happy path and money rules | Money movement | Not started | | |
| 06 | Concurrency: locks and lock ordering | Money movement | Not started | | |
| 07 | Idempotency keys | Money movement | Not started | | |
| 08 | Statements, error format and Swagger | Money movement | Not started | | |
| 09 | Risk rules and freezing | Money movement | Not started | | |
| 10 | Reversals and append-only ledger | Money movement | Not started | | |
| 11 | Transactional outbox | Events | Not started | | |
| 12 | Kafka and the outbox relay | Events | Not started | | |
| 13 | Audit consumer | Events | Not started | | |
| 14 | Reconciliation | Events | Not started | | |
| 15 | JWT login and roles | Security | Not started | | |
| 16 | Console: scaffold, login, account list | Console | Not started | | |
| 17 | Console: account page | Console | Not started | | |
| 18 | Console: transfers and audit trail | Console | Not started | | |
| 19 | Console: transfer simulator | Console | Not started | | |
| 20 | Dockerize the whole stack | Ship | Not started | | |
| 21 | Deploy to AWS EC2 | Ship | Not started | | |
| 22 | API Gateway, HTTPS and Netlify | Ship | Not started | | |
| 23 | README and demo video: v1 done | Ship | Not started | | |
| S1 | S3 audit archive with Object Lock | Stretch | Not started | | |
| S2 | Nightly reconciliation Lambda | Stretch | Not started | | |
| S3 | Least-privilege IAM roles | Stretch | Not started | | |
| S4 | Infrastructure as code | Stretch | Not started | | |
| S5 | Load test with k6 | Stretch | Not started | | |

## How to run a build

Each build follows the same loop in Claude Code; it matches "How we work" in `CLAUDE.md`. Skill names come from `mattpocock/skills`; if your installed version names them differently, check `/help`.

Every build leaves two logs: a public one in `docs/build-log/NN-short-name.md` (committed, for recruiters and reviewers) and a private learning log in `.learning/NN-short-name.md` (git-ignored, for you). `CLAUDE.md` lists the sections of each.

1. **Prepare (you, ~15 min).** Read the build's "Learn first" items in your learning guide. Then `/clear` to start a fresh session.
2. **Plan (Claude).** Prompt:
   > Read CLAUDE.md, docs/PRD.md, GLOSSARY.md, Build NN in BUILDS.md and the previous build's public log in docs/build-log/. Explain the plan in plain words: what this build adds, which files change, which tests you'll write first and why. Wait for my go.

   Ask questions until the plan makes sense. Push back if it does more than the build's scope. Say "go" when it does.
3. **Concept primer (Claude).** Before any code, Claude explains this build's new concepts in plain words with a small FinLedger example and writes them to the "Before" section of the private log. Ask until they make sense, then say "ready".
4. **Build (Claude, test-first).** After "ready", `/tdd Build NN`. Watch the tests fail, then pass.
5. **Understand (you).** Ask Claude to walk through every changed file (`/zoom-out` helps); it records the walkthrough in the private log. Run the app and tests yourself.
6. **Quiz (you).** Claude asks the build's review questions one at a time, waits for your answer and gives honest feedback; it never answers for you. Questions, your answers word for word and the feedback go into the private log. If you get one wrong, ask about it and try again. Don't move on until you can answer them all.
7. **Close.** Claude updates the tracker, writes the public log, finishes the private log and adds new terms to `GLOSSARY.md`, then proposes a commit message. It doesn't commit: you review, commit and push. If something broke, `/diagnose` before moving on.

If a build runs well over 2 hours, stop and split it: finish what's working, write the rest down as a new build.

---

## Phase 0 — Foundation

### Build 00 — Repo and workflow setup
**Goal:** an empty repo with the plan files and the AI workflow ready. *You do this one by hand.*
- Create a GitHub repo `finledger` (public, so recruiters can see it) and clone it.
- Add `CLAUDE.md`, `BUILDS.md`, `docs/PRD.md`, a Java + Node `.gitignore`, and a one-line `README.md`.
- Install the skills: `npx skills@latest add mattpocock/skills` (choose Claude Code). Run `/setup-matt-pocock-skills` and pick local markdown for issues.
- Optional but valuable: run `/grill-with-docs` on `docs/PRD.md` and let it challenge the plan. Update the PRD with anything you change.

**Done when:** the repo is on GitHub with the four files, Claude Code lists the skills, and the first two build logs exist: `docs/build-log/00-setup.md` (public) and `.learning/00-setup.md` (private).
**Review:** What does `CLAUDE.md` do that `PRD.md` doesn't? Why start every build with a fresh session?

### Build 01 — Spring Boot skeleton + Postgres in Docker
**Learn first:** Docker in 100 Seconds; what Spring Boot auto-configuration is.
- Generate `core/` at start.spring.io: Java 21, Maven, Web, Data JPA, PostgreSQL, Flyway, Validation, Actuator, Testcontainers.
- `infra/docker-compose.yml` with Postgres 16.
- App connects to Postgres; `/actuator/health` returns `UP`.

**Done when:** `docker compose up -d` then `./mvnw spring-boot:run` shows health `UP`; the default context test passes.
**Review:** What is the difference between an image and a container? Where does Spring Boot get the database URL from? What happens if Postgres isn't running?

### Build 02 — Accounts: first table, first API, first real test
**Learn first:** Flyway basics; DTO vs entity; Testcontainers guide.
- Flyway `V1__create_accounts.sql` in schema `ledger` with `NUMERIC(19,4)` balance.
- `POST /api/v1/accounts` and `GET /api/v1/accounts/{id}` with record DTOs and validation.
- Testcontainers integration test: create then fetch an account; invalid input → 400.

**Done when:** both endpoints work in Postman or curl and the test passes against a real Postgres.
**Review:** Why `NUMERIC(19,4)` and not `DOUBLE PRECISION`? Why doesn't the controller return the entity? What does Testcontainers do when the test starts?

### Build 03 — CI: tests run on every push
**Learn first:** what CI is; GitHub Actions workflow file structure.
- `.github/workflows/ci.yml`: checkout, set up Java 21, cache Maven, `./mvnw verify` in `core/`.
- Add the CI badge to `README.md`.

**Done when:** a push shows a green check on GitHub; a deliberately broken test turns it red (then fix it).
**Review:** Why does Testcontainers work inside GitHub Actions? Why set up CI this early instead of at the end?

---

## Phase 1 — Money movement

### Build 04 — Deposits and withdrawals with double-entry
**Learn first:** Accounting for Developers chapters 1–2; ACID.
- Migrations: `transfers`, `ledger_lines`; seed the System Account (`SYSTEM`).
- `POST /deposits` and `POST /withdrawals` as Transfers from/to the System Account, inside `@Transactional`.
- Withdrawal beyond balance → `INSUFFICIENT_FUNDS`.
- Invariant test helper: all ledger lines sum to 0; each balance equals the sum of its lines. Reuse it in every later build.

**Done when:** deposit RM100, withdraw RM30 → balance RM70, four ledger lines, invariant holds.
**Review:** Why does a Deposit need the System Account? What would break if the balance update succeeded but the ledger insert failed?

### Build 05 — Transfers: happy path and money rules
**Learn first:** BigDecimal pitfalls (concept 4 in your guide).
- `POST /transfers` between two customer accounts: amount > 0, max 2 decimals, both accounts active, same currency.
- `Money` helper: parse from String, scale 2, `compareTo` comparisons.
- Unit tests for validation; integration test for the transfer; rounding acceptance test.

**Done when:** acceptance test 4 (rounding) passes; a transfer writes one transfer row and two ledger lines.
**Review:** Why build `BigDecimal` from a String? Why does `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` return false?

### Build 06 — Concurrency: locks and lock ordering
**Learn first:** Baeldung pessimistic locking; concepts 5 and 6.
- Repository method with `@Lock(PESSIMISTIC_WRITE)` and a lock timeout.
- Transfer service locks both accounts in ascending ID order.
- Concurrency tests with `ExecutorService` + `CountDownLatch`.

**Done when:** acceptance tests 1 (double-spend) and 2 (deadlock) pass. Bonus: temporarily remove the lock or the ordering, watch them fail, then restore.
**Review:** Explain the double-spend in a timeline of two requests. Why does locking in ID order prevent deadlock? Why pessimistic instead of optimistic locking here?

### Build 07 — Idempotency keys
**Learn first:** Stripe's idempotency article.
- Required `Idempotency-Key` header on deposits, withdrawals and transfers (and, from Build 10, reversals); unique per client on (`client_id`, `idempotency_key`), plus a `request_hash` that includes the Transfer's `kind` (ADR 0003).
- Replay with same body → stored result, `200`. Same key, different body → `422 IDEMPOTENCY_KEY_REUSED`.
- Handle two concurrent requests with the same key (unique constraint wins; the loser returns the stored result).

**Done when:** acceptance test 3 passes, including a parallel version.
**Review:** Who generates the key, client or server, and why? What happens if two identical requests arrive at the same millisecond?

### Build 08 — Statements, error format and Swagger
**Learn first:** RFC 7807 Problem Details; OpenAPI basics.
- `GET /accounts/{id}/statement` with pagination and running balance (`balance_after`).
- Global exception handler returning Problem Details with `reasonCode`.
- springdoc-openapi: Swagger UI at `/swagger-ui.html`.

**Done when:** every error in the app returns the same JSON shape; Swagger lists all endpoints.
**Review:** Why is a stable `reasonCode` better than an error message string for client systems?

### Build 09 — Risk rules and freezing
- Per-transfer limit and daily outgoing limit, configurable in `application.yml`.
- `POST /accounts/{id}/freeze` and `/unfreeze`; a Frozen account blocks every Deposit, Withdrawal and Payment into or out of it.
- Rejected transfers stored with `status = REJECTED` and reason code.

**Done when:** tests cover `LIMIT_EXCEEDED` (Per-Transfer Limit), `DAILY_LIMIT_EXCEEDED` (Daily Limit) and `ACCOUNT_FROZEN`, and rejected transfers are queryable.
**Review:** Why must the daily-limit check happen after the accounts are locked? Why store rejected transfers at all?

### Build 10 — Reversals and append-only ledger
**Learn first:** concept 10 (immutability).
- `POST /transfers/{id}/reverse` creates an opposite transfer with `reversal_of_id`; a transfer can only be reversed once.
- Migration: separate DB user for the app with UPDATE/DELETE revoked on `ledger_lines` (and later `audit_log`).
- `GET /transfers?accountId=&status=` search.

**Done when:** a test proves an UPDATE on `ledger_lines` fails with a permission error, and reversal keeps the invariant.
**Review:** Why is "enforced in the database" stronger than "enforced in Java"? How would an auditor see that a reversal happened?

---

## Phase 2 — Events

### Build 11 — Transactional outbox
**Learn first:** microservices.io transactional outbox; the dual-write problem.
- Migration `outbox_events`; every completed or rejected transfer inserts an event in the same transaction.
- Event JSON: `eventId`, `type`, `transferId`, accounts, amount, status, `occurredAt`, `correlationId`.

**Done when:** a test proves that if the transfer rolls back, no outbox row exists, and if it commits, exactly one does.
**Review:** Why not just call Kafka after `save()`? What does "the event is saved with the money" guarantee?

### Build 12 — Kafka and the outbox relay
**Learn first:** Kafka in 100 Seconds, then Kafka in 6 minutes.
- Kafka (KRaft, single node) in Compose; topic `transfer-events`, keyed by transfer ID.
- `@Scheduled` relay: read unpublished rows (oldest first, batch), publish, mark `published_at`.
- Testcontainers Kafka test.

**Done when:** acceptance test 5 passes: with Kafka stopped Transfers complete; after restart every event is published.
**Review:** If the relay crashes after sending but before marking published, what happens? Why is that acceptable?

### Build 13 — Audit consumer
- New Maven module `audit-consumer`: `@KafkaListener` inserting into `audit.audit_log` by `event_id` (`ON CONFLICT DO NOTHING`).
- `GET /api/v1/audit?transferId=` served by the consumer.
- Its own DB user that can only write the `audit` schema.

**Done when:** acceptance test 6 (duplicate event) passes, and an end-to-end test shows transfer → audit row.
**Review:** Explain at-least-once delivery plus an idempotent consumer. Why is the consumer a separate app?

### Build 14 — Reconciliation
- `GET /admin/reconciliation`: ledger sums to zero; each balance equals its lines; every Transfer, completed or rejected, has an audit row (via the audit API).
- Returns pass/fail per check with the mismatching IDs.

**Done when:** acceptance test 7 is wired into the suite and a test that corrupts a balance makes reconciliation fail.
**Review:** Why does reconciliation report problems instead of fixing them?

---

## Phase 3 — Security

### Build 15 — JWT login and roles
**Learn first:** Spring Security JWT docs; concept 11.
- `app_users` table with BCrypt hashes; seed one user per role from environment variables.
- `POST /auth/login` issues a signed JWT; every other endpoint requires it.
- Role rules per the PRD API table; security tests for 401 and 403.

**Done when:** every endpoint has at least one test for the right role and one for a wrong role.
**Review:** What's inside a JWT and why can the server trust it without a DB lookup? Difference between 401 and 403?

---

## Phase 4 — Console

### Build 16 — Console: scaffold, login, account list
- `console/` with Vite + React + TypeScript; API base URL from an env variable.
- Login page storing the token in memory; account search list.
- CORS configured in core for the console's origin.

**Done when:** you can log in locally and see accounts; a Vitest test covers the login form.
**Review:** What is CORS and why does the browser enforce it, not the server?

### Build 17 — Console: account page
- Account detail: balance, status, paginated statement; freeze/unfreeze for `OPS`.

**Done when:** freezing in the UI makes a transfer fail with `ACCOUNT_FROZEN`.

### Build 18 — Console: transfers and audit trail
- Transfer search with status filter; reverse button; audit trail view (calls audit-consumer).

**Done when:** reversing in the UI shows the new transfer and both audit events.

### Build 19 — Console: transfer simulator
- Core: `POST /admin/simulations` (`ADMIN`) creates two fresh demo accounts, deposits RM500 into one, fires N parallel Payments on the server, and returns the counts and final balance.
- Simulator page: a "double-spend" button calls that endpoint and shows the result summary.

**Done when:** the button shows exactly the successes the balance allows, and reconciliation still passes.
**Review:** Explain to an imaginary recruiter what the double-spend button proves.

---

## Phase 5 — Ship

### Build 20 — Dockerize the whole stack
- Multi-stage Dockerfiles for `core` and `audit-consumer`; cap JVM heap (e.g. `-Xmx256m`).
- Compose runs Postgres, Kafka, core and audit-consumer; CI also builds the images.

**Done when:** a fresh clone + `docker compose up` gives a working system with no local Java needed.
**Review:** What does a multi-stage build save? Why cap the heap?

### Build 21 — Deploy to AWS EC2
**Learn first:** your AWS account's Free plan rules; set a budget alert before anything else.
- One EC2 instance (about 4 GB RAM) with Docker; copy Compose and `.env`; security group allowing only what's needed.

**Done when:** the API answers from the instance's address; the budget alert is set; you can stop and restart it safely.
**Review:** Which ports are open and why? Where do the production secrets live?

### Build 22 — API Gateway, HTTPS and Netlify
- API Gateway HTTP API in front of EC2 with throttling; HTTPS URL for the API.
- Connect the repo to Netlify, build `console/`, set the API URL env variable; production deploys from `main` only.
- Update CORS for the Netlify domain.

**Done when:** the Netlify console works end to end against the AWS API.
**Review:** Why does the console live on Netlify while the API lives on AWS? What does API Gateway add over hitting EC2 directly?

### Build 23 — README and demo video: v1 done
- README: pitch, architecture diagram, acceptance tests + CI badge, "how it handles failure", quick start, live links, design decisions, out of scope.
- 2–3 minute video: double-spend, idempotent retry, Kafka down and catching up, reconciliation.

**Done when:** a friend who has never seen the project understands it from the README in 5 minutes.

---

## Stretch (after v1, any order)

### S1 — S3 audit archive with Object Lock
Audit consumer also writes each event to a versioned S3 bucket with Object Lock. **Done when:** deleting an archived event fails.

### S2 — Nightly reconciliation Lambda
Scheduled Lambda calls `/admin/reconciliation` and saves the report to S3. **Done when:** a report appears each morning.

### S3 — Least-privilege IAM roles
One IAM role per component (EC2 app, Lambda), policies in `infra/iam/` with a comment on every permission. **Done when:** removing any one permission breaks exactly the feature it explains.

### S4 — Infrastructure as code
Terraform or AWS CDK for EC2, API Gateway, S3 and Lambda. **Done when:** `destroy` then `apply` rebuilds the environment.

### S5 — Load test with k6
k6 script for transfers; publish p95 latency and throughput in the README. **Done when:** numbers are in the README with the instance size used.
