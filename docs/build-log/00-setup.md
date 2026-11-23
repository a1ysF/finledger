# Build 00: Repo and workflow setup

## Summary

Set up the FinLedger repository, its planning documents and an AI-assisted, test-first workflow before writing any application code. The specification was stress-tested in a structured design review, which produced a shared domain glossary and more than twenty decisions on idempotency, Reversals, risk rules and the System Account.

## What was built

- Repository scaffold: a `.gitignore` for Java/Maven, Node, VS Code and secrets (`.env` ignored, `.env.example` kept), and a one-line `README.md`.
- Planning documents versioned with the code: `CLAUDE.md` (working rules), `docs/PRD.md` (specification), `BUILDS.md` (build plan and tracker).
- `GLOSSARY.md`: one canonical name for each domain concept (Transfer, Deposit, Withdrawal, Payment, Reversal, System Account, Idempotency Key and others), with the synonyms to avoid.
- Five architecture decision records in `docs/adr/`: modular monolith, transactional outbox, Idempotency Key scope, Reversal exemptions, and lock ordering.
- Agent skills configuration in `docs/agents/`: a local-markdown issue tracker under `.scratch/`, the default triage labels, and a single-context layout for domain docs.
- Two logs per build: public build logs in `docs/build-log/` and private learning logs in a git-ignored `.learning/`.
- No endpoints yet.

## Key decisions and trade-offs

- **Planning documents live inside the repository.** They are reviewed and versioned alongside the code they describe. Alternative: keep them in a folder beside the repo, where they would never reach reviewers.
- **Issues are tracked as local markdown files.** This suits a solo project and needs no extra tooling. Alternative: GitHub Issues, which are more visible but add a CLI and a sync step.
- **Transfer is the umbrella term.** Every money-moving operation is a Transfer of one of four kinds (Deposit, Withdrawal, Payment, Reversal), stored in one table and run through one locking and ledger path. Alternative: a separate word for the umbrella, which would split one code path across two vocabularies.
- **Idempotency Keys are scoped per client and never expire, and a Rejected Transfer uses up its key.** A retry always returns the original result, including a rejection. Alternative: free the key after a rejection. That is friendlier to clients, but a retry could then complete a Transfer the client had already been told was rejected.
- **Reversals ignore freezes and limits, but not insufficient funds.** Operations staff can pull stolen money out of a Frozen account, and a Customer Account still never goes negative. Alternative: a freeze with no exceptions, which would block fraud recovery.
- **The concurrency simulator runs on the server** (`POST /admin/simulations`, `ADMIN` role). The parallelism is real and no client-system credential reaches the browser. Alternative: fire the requests from the browser, which caps connections and needs a `SERVICE` login in the console.

## Concepts demonstrated

| Concept | Where it lives |
| --- | --- |
| Ubiquitous language: one name per domain concept | `GLOSSARY.md` |
| Specification before code | `docs/PRD.md`: user stories and acceptance tests |
| Persistent instructions for an AI agent | `CLAUDE.md`: "Non-negotiable rules" and "How we work" |
| Secrets kept out of version control | `.gitignore`: "Secrets / environment" section |
| Tool configuration as code | `docs/agents/issue-tracker.md`, `triage-labels.md`, `domain.md` |

## How it's tested

Build 00 adds no application code, so there are no automated tests yet. Setup checks:

- `git check-ignore -v .learning/test.md` shows that the `.learning/` rule in `.gitignore` matches, so private logs are never committed.
- `/setup-matt-pocock-skills` completed, and Claude Code lists the installed skills.

The first automated test, the Spring context test, arrives in Build 01.

## Next

Build 01 adds a Spring Boot skeleton in `core/`, connected to PostgreSQL 16 in Docker, with `/actuator/health` returning `UP`.
