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

Coverage report: `services/onboarding-api/target/site/jacoco/index.html`.

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
