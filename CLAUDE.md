# FinLedger Core

A money-movement engine for a digital bank: it moves funds between accounts without losing, duplicating or miscounting a single sen, and keeps a tamper-proof audit trail of every Transfer. It is a portfolio project built to learn enterprise backend engineering, so **understanding beats speed**.

- Full spec: `docs/PRD.md`
- Build plan and progress: `BUILDS.md`

## Stack

Java 21 · Spring Boot 3 · Maven · PostgreSQL 16 · Flyway · Apache Kafka (single node, KRaft) · JUnit 5 · Mockito · Testcontainers · React + TypeScript (Vite) · Docker Compose · GitHub Actions · AWS (EC2, API Gateway; S3 and Lambda later) · Netlify (console hosting)

## Repo layout (grows build by build)

```
core/              Spring Boot: API, transfer engine, outbox relay
audit-consumer/    Spring Boot: Kafka listener, audit log, audit API
console/           React + TypeScript ops console
infra/             docker-compose.yml, IAM policies, (later) Terraform
docs/              PRD.md, decision records (docs/adr/)
docs/agents/       Agent skills config: issue tracker, triage labels, domain docs
docs/build-log/    Public build logs, one per build (NN-short-name.md)
.learning/         Private learning logs, one per build (git-ignored, private)
.github/workflows/ CI pipeline
GLOSSARY.md        Domain language: the terms every doc, class and test uses
```

## Commands

Fill these in as each build adds them.

| What | Command |
| --- | --- |
| Start local stack | `docker compose -f infra/docker-compose.yml up -d` |
| Run core tests | `cd core && ./mvnw verify` |
| Run audit tests | _(Build 13)_ |
| Run console | _(Build 16)_ |

## Non-negotiable rules

These protect the money. Never break them, even in a test or a "quick fix".

1. **Money is never `double` or `float`.** Java: `BigDecimal`, built from a `String`, scale 2 for MYR, compared with `compareTo`. Postgres: `NUMERIC(19,4)`.
2. **Every transfer is one database transaction** (`@Transactional` on a public service method called from another bean).
3. **Lock accounts in ascending ID order** with `SELECT … FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`), never sender-first.
4. **Every money-moving request needs an `Idempotency-Key`.** Same key + same body → stored result. Same key + different body → reject.
5. **Double-entry always:** each completed transfer writes exactly two ledger lines that sum to zero. Deposits and withdrawals go through the System Account (`SYSTEM`).
6. **Ledger and audit rows are append-only.** Mistakes are undone by Reversals (a new opposite Transfer), never UPDATE or DELETE.
7. **Events go through the outbox table** in the same transaction as the money. Never call Kafka directly from the transfer code.
8. **Consumers are idempotent:** a duplicate event must not create a duplicate row.
9. **The API speaks DTOs only.** Never return a JPA entity. Validate input at the edge.
10. **Schema changes only through new Flyway migrations.** Never edit a migration that has run.

## How we work (read this before every build)

The owner is a QA engineer moving into backend development. Every session must leave them understanding the code, not just having it.

Every build leaves two logs, filled in as the build goes:

- **Public log** `docs/build-log/NN-short-name.md` (committed; for recruiters and reviewers; concise, no rough notes): Summary · What was built · Key decisions and trade-offs · Concepts demonstrated · How it's tested · Next.
- **Private learning log** `.learning/NN-short-name.md` (git-ignored; for the owner): Before · Code walkthrough · How it works step by step · Try it yourself · Quiz · Still confusing / to revisit · Glossary terms added this build.

1. **One build per session.** Start from a fresh context. Read this file, `docs/PRD.md`, `GLOSSARY.md`, the build's section in `BUILDS.md` and the previous build's public log in `docs/build-log/`. Do not start the next build unless asked.
2. **Plan first, then wait.** Before writing code, explain in plain words: what this build adds, which files change, which tests come first and why. Then stop and wait for "go".
3. **Concept primer, then wait.** Before any code, explain each concept that is new in this build in plain words, with a small FinLedger example (e.g. two requests on a timeline). Write it to the "Before" section of the private log. Then stop and wait until the owner says "ready".
4. **Test first** (use `/tdd`): failing test → minimal code → refactor. Run the tests and show they pass.
5. **Explain after.** For every file changed, give one or two sentences on what it does and why. Name the concept it demonstrates (e.g. "this is lock ordering") and point to where it lives in the code. Record the walkthrough in the private log ("Code walkthrough", "How it works step by step", "Try it yourself").
6. **Quiz.** Ask the build's review questions one at a time. Wait for the owner's answer, then give honest feedback: what's right, what's missing, what's wrong. Never answer for them. Record each question, their answer word for word, and the feedback in the private log's "Quiz" section.
7. **Never weaken a test to make it pass.** Never skip, disable or delete tests. If stuck, stop and explain the blocker (`/diagnose`).
8. **Ask before** adding a dependency, changing the schema outside the build's scope, or changing anything in this file's rules.
9. **Finish the build:** update the tracker table in `BUILDS.md` (status, date, notes), write the public log, finish the private log (including "Still confusing / to revisit" from the owner's questions this session), and add any new terms to `GLOSSARY.md`. Then propose a commit message. Don't commit: the owner reviews and commits.

## Conventions

- Packages by feature: `com.finledger.core.account`, `.transfer`, `.ledger`, `.outbox`, `.security`.
- Layers inside a feature: `Controller` → `Service` → `Repository`; DTOs as Java `record`s.
- Errors: RFC 7807 Problem Details with a stable `reasonCode` (e.g. `INSUFFICIENT_FUNDS`).
- Commits: `build-05: transfers with lock ordering` style, small and focused.
- Secrets only in environment variables or a git-ignored `.env`; never in Git.

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/` in this repo. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default triage roles, each written as a `Status:` line in the issue file. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one root `GLOSSARY.md` plus `docs/adr/`. See `docs/agents/domain.md`.
