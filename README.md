# onboarding-platform

[![CI](https://github.com/OWNER/onboarding-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/onboarding-platform/actions/workflows/ci.yml)
[![CodeQL](https://github.com/OWNER/onboarding-platform/actions/workflows/codeql.yml/badge.svg)](https://github.com/OWNER/onboarding-platform/actions/workflows/codeql.yml)

Customer onboarding platform for a fictional bank: a set of Java/Spring Boot services on Azure that
take a new customer from application to an open account, by integrating with (simulated) external
systems for identity checks (KYC), credit scoring, digital signing and core banking.

The project is a hands-on portfolio for enterprise integration: REST and event-driven integration,
Azure Service Bus, resiliency, observability, security, Terraform and CI/CD.

## Repository layout

| Path | Contents |
|---|---|
| `services/onboarding-api` | Entry point for onboarding applications (web, mobile, partners) |
| `services/onboarding-processor` | Azure Function that consumes `ApplicationSubmitted` events from Service Bus |
| `services/library-api` | Book catalogue CRUD API on Postgres (Flyway) with a transactional outbox for book events |
| `simulators/` | Stubs of external systems (KYC, credit bureau, core banking, e-signing) – coming |
| `infra/terraform/` | Azure infrastructure as code – coming |
| `docs/adr/` | Architecture decision records |
| `docs/architecture/` | C4 diagrams – coming |
| `.github/` | CI/CD workflows, Dependabot, PR template |

## Build and run locally

Requirements: Java 21, Docker (for later integration tests). Maven is provided by the wrapper.

```bash
./mvnw verify                                        # build, test, coverage (Windows: mvnw.cmd verify)
./mvnw -pl services/onboarding-api spring-boot:run   # start the API on http://localhost:8080
```

Try it:

```bash
curl -i -X POST http://localhost:8080/api/v1/applications \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com"}'

curl http://localhost:8080/actuator/health
```

The POST returns `202 Accepted` with an `applicationId` and publishes an `ApplicationSubmitted` event.
Locally Service Bus is off by default and the event is only logged. To publish to the real queue
(uses your `az login`, which has *Azure Service Bus Data Owner* on the namespace):

```bash
SERVICEBUS_ENABLED=true SERVICEBUS_NAMESPACE=sb-ivaninterview-dev-bec \
./mvnw -pl services/onboarding-api spring-boot:run
```

In Azure the event is consumed by `onboarding-processor`, which logs `Processed application <id>`.
If the event cannot be published, the API returns `503 Service Unavailable` and the caller should retry.

Coverage report: `services/onboarding-api/target/site/jacoco/index.html`.

### library-api

Spring Boot starts a local Postgres 17 container from `services/library-api/compose.yaml`, and Flyway
creates the schema from `src/main/resources/db/migration`.

```bash
./mvnw -pl services/library-api spring-boot:run   # start the API on http://localhost:8081
```

Try it with [`services/library-api/library-api.http`](services/library-api/library-api.http)
(run the requests from IntelliJ or VS Code REST Client), or with curl:

```bash
curl -i -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Dune","author":"Frank Herbert","isbn":"9780441013593","publishedYear":1965}'

curl http://localhost:8081/api/v1/books
```

Every create, update and delete also writes a `BookCreated`, `BookUpdated` or `BookDeleted` event to the
`outbox_events` table, in the same transaction as the change to `books`. A failed change (for example a
duplicate ISBN) leaves neither a book nor an event. To look at the tables while the app is running:

```bash
cd services/library-api
docker compose exec postgres psql -U library -d library
```

```sql
SELECT * FROM books;
SELECT event_type, aggregate_id, payload, published_at FROM outbox_events ORDER BY created_at;
SELECT version, description, success FROM flyway_schema_history;
```

`published_at` stays empty until the outbox relay is added.

If Flyway refuses to start with *non-empty schema without schema history table*, the container still has
tables that Hibernate created before Flyway was introduced: run `docker compose down` in
`services/library-api` once (the local data is thrown away).

## CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | push to `main`, PRs | Build, unit tests, JaCoCo coverage, SonarCloud quality gate (when configured) |
| `codeql.yml` | push, PRs, weekly | Static security analysis of the Java code and the workflows |
| `dependency-review.yml` | PRs | Blocks new dependencies with high-severity vulnerabilities or disallowed licences |
| Dependabot | weekly | PRs for Maven and GitHub Actions updates |

## Way of working

Trunk-based development with short-lived branches and pull requests, Conventional Commits.
See [ADR-0001](docs/adr/0001-monorepo-and-trunk-based-development.md).
