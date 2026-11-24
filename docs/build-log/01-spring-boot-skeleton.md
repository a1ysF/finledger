# Build 01: Spring Boot skeleton + Postgres in Docker

## Summary

Added the runnable foundation every later build sits on: a Spring Boot 4.2 service in `core/` that connects to PostgreSQL 16 running in Docker and reports its health, including the database's, at `/actuator/health`. The first automated tests run against a real, throwaway Postgres started by Testcontainers.

## What was built

- `infra/docker-compose.yml`: PostgreSQL 16 (`postgres:16-alpine`) with a named volume and a `pg_isready` health check. It fails fast with a clear message if a required setting is missing.
- `infra/.env.example`: a committed template. The real `infra/.env` is git-ignored and is the single source of local database settings for both Docker Compose and the application.
- `core/`: a Spring Boot 4.2.0 application (Java 21, Maven wrapper) with Web MVC, Data JPA, PostgreSQL driver, Flyway, Validation and Actuator.
- `application.yml`: the database URL and credentials come only from placeholders (`${DB_NAME}`, `${DB_USERNAME}`, `${DB_PASSWORD}`); Hibernate schema generation is off (`ddl-auto: validate`); open-session-in-view is disabled; health components are shown.
- Tests: a context-load smoke test and a health endpoint test, both on a Testcontainers Postgres.
- No domain tables or business endpoints yet.

## Key decisions and trade-offs

- **Spring Boot 4.2 instead of 3.x.** Open-source support for 3.5, the last 3.x line, ended on 2026-06-30, and start.spring.io no longer generates it. A service that moves money should run on a framework that still receives security fixes. Trade-off: most tutorials target Boot 3, and some names differ (modular starters such as `spring-boot-starter-webmvc` and `spring-boot-starter-flyway`, Testcontainers 2 packages, JUnit 6). The project docs were updated to match.
- **One git-ignored `.env` for Compose and Spring.** Spring imports it with `spring.config.import: optional:file:../infra/.env[.properties]`, so local credentials are defined once, with nothing secret in Git. Real environment variables take precedence, which is how production will supply them. Alternative: export variables by hand before every run, which is more friction and easier to get out of sync with the database.
- **Tests use their own Postgres container, not the Compose one.** `@ServiceConnection` replaces the configured URL and credentials with those of a fresh container on a random port. Tests always start clean and never touch development data. Alternative: H2 in memory, which is faster but behaves differently for row locks and `NUMERIC`, exactly where FinLedger's money rules live.
- **The same image tag in Compose and tests.** The generated test configuration used `postgres:latest`; it is pinned to `postgres:16-alpine` so tests run on the version the app is developed and deployed against.
- **Hibernate never touches the schema.** `ddl-auto: validate` leaves schema changes to Flyway migrations only (project rule 10).

## Concepts demonstrated

| Concept | Where it lives |
| --- | --- |
| Containers, images and volumes | `infra/docker-compose.yml` |
| Externalized configuration and placeholders | `core/src/main/resources/application.yml` |
| Secrets kept out of Git | `infra/.env.example` committed, `infra/.env` ignored |
| Spring Boot auto-configuration | `CoreApplication.java` (`@SpringBootApplication`) |
| Health checks with component breakdown | `management.endpoint.health.show-components` in `application.yml` |
| Integration testing against real infrastructure | `TestcontainersConfiguration.java` (`@ServiceConnection`) |
| Fail fast on missing infrastructure | App refuses to start without a reachable database (Flyway connects at startup) |

## How it's tested

`cd core && ./mvnw verify`: 2 tests, 0 failures. Docker must be running.

- `HealthEndpointTest`: `GET /actuator/health` returns `200` with `status: UP` and `components.db.status: UP`. Written first; it failed until health components were exposed, which proves it checks the database specifically and not just that the app is alive.
- `CoreApplicationTests`: the Spring context starts with the full configuration.

Manual check: with `docker compose -f infra/docker-compose.yml up -d` and `./mvnw spring-boot:run`, the app connects to `localhost:5432/finledger` (PostgreSQL 16) and `/actuator/health` reports `db: UP`.

## Next

Build 02 adds the first Flyway migration (`accounts` in schema `ledger`, `NUMERIC(19,4)` balance) and the first API: create and fetch a Customer Account, with record DTOs, validation and a Testcontainers integration test.
