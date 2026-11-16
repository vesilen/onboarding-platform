# ADR-0001: Monorepo and trunk-based development

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

The onboarding platform will consist of several services (onboarding API, audit service, ...),
simulators of external systems (KYC provider, credit bureau, core banking, e-signing) and
infrastructure code (Terraform). It is built and maintained by a single developer, but should be
run the way an enterprise team would run it: reviewed changes, automated quality gates, and an
always-releasable main branch.

## Decision

1. **One repository** (monorepo) for all services, simulators, infrastructure and documentation,
   with a Maven parent pom that manages Java and dependency versions centrally.
2. **Trunk-based development:** `main` is always releasable. Work happens on short-lived branches
   (`feat/...`, `fix/...`) and lands through a pull request that must pass CI.
3. **Conventional Commits** (`feat:`, `fix:`, `chore:`, ...) so versioning and changelogs can be
   automated later.

## Consequences

- A change that touches several services (for example an event contract and its consumer) is one
  atomic pull request.
- One place for issues, pipelines, ADRs and the README, which also makes the project easy to show.
- The CI pipeline builds the whole reactor. Once there are many services, add path filters or
  reusable per-service workflows so only changed services are built and deployed.
- Services share a release cadence by default; independent versioning per service needs extra
  tooling if it becomes necessary.
