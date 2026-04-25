# AegisNexus Agent Guide

> This file is the single source of truth for all AI coding agents working in this repository. `CLAUDE.md`, Cursor Skills, and ad-hoc prompts must point here instead of maintaining conflicting rules.

## 1. Language Policy

- Repository language: English.
- Documentation, code comments, commit messages, PR descriptions, configuration comments, and Skill content must be written in English.
- User-facing product text must still go through i18n keys.
- Chat with the owner in Simplified Chinese unless the owner asks otherwise.
- If existing files contain Chinese prose or comments, convert them to English when touching the same area. Do not perform a whole-repository language rewrite unless explicitly requested.

## 2. Project Mission

AegisNexus is an enterprise low-code AI Agent orchestration platform for business teams and platform operators. Core capabilities:

- Visual Agent / Workflow orchestration.
- Unified API and tool lifecycle management.
- MCP / HTTP / Function tool integration.
- Multi-tenancy, RBAC, audit, quota, and security compliance.
- Observable, deployable, and continuously evolvable runtime.

The current direction is **v2 Core refactoring**, not a full rewrite: preserve product assets while rebuilding the designer foundation, Graph DSL, Runtime, Tool Plane, and verification gates.

## 3. Required Reading

Read documents by task type. Do not load all documentation into context by default.

- Roadmap: `docs/PLATFORM_V2_EXECUTION_PLAN.md`
- Documentation index: `docs/README.md`
- Start point: `docs/START_HERE.md`
- Known issues: `docs/KNOWN_ISSUES.md`
- Architecture decisions: `docs/DECISIONS.md`
- Lessons learned: `docs/LESSONS_LEARNED.md`
- Architecture: `docs/architecture.md`
- Security baseline: `docs/security.md`
- Deployment: `DEPLOYMENT.md`, quick reference in `docs/deployment.md`
- API list: `backend/src/main/resources/api-changelog.md`
- Backend notes: `backend/README.md`
- Contribution and verification: `CONTRIBUTING.md`

## 4. Tech Stack

- Frontend: Vue 3, TypeScript, Vite, Ant Design Vue, Tailwind CSS, Pinia, vue-i18n.
- Backend: Spring Boot 3, Java 17, Spring Security, JPA, Flyway, Redis, LangChain4j.
- Infrastructure: PostgreSQL, Redis, Docker Compose, Prometheus, Grafana, GitHub Actions.

## 5. Non-Negotiable Engineering Rules

- API responses use the `{ code, message, data }` family of wrappers; pagination uses `PageResult`.
- Backend pagination is 0-based; frontend pagination is 1-based. Convert at the API/page boundary.
- Controllers must not expose Entities directly; use DTOs/VOs.
- Tenant isolation must be explicit. Never bypass `tenantId`, permissions, or audit trails.
- Secrets must come from environment variables or a secrets manager. Never commit real secrets.
- User-visible frontend text must use i18n keys.
- Database schema changes require Flyway migrations.
- Never weaken production security to make stale tests pass.
- Refactor poor-quality code when it blocks safe evolution. Do it in scoped, reversible steps with verification.

## 6. AI Execution Rules

Each run handles one task package only. Before coding, state:

```markdown
Task:
Goal:
Allowed files:
Forbidden changes:
Required reading:
Verification commands:
Risks:
```

Execution order:

1. Read the task and directly related files.
2. Select relevant project Skills from `.cursor/skills`.
3. Make small, reviewable changes.
4. Run the narrowest useful verification.
5. Report changes, verification, and remaining risks.

Forbidden:

- Changing designer, runtime, database, and CI in one task.
- Large formatting-only diffs that hide real changes.
- Reverting user changes.
- Adding unused dependencies.
- Presenting target-state documentation as completed implementation.

## 7. Recommended Skills

- `platform-v2-execution-plan`: phase planning and task decomposition.
- `platform-v2-startup-check`: session startup assessment before implementation.
- `platform-v2-architecture-review`: architecture review and refactor decisions.
- `platform-v2-architecture-stewardship`: global architecture guardrails and technical debt reduction.
- `platform-v2-safe-implementation`: safe implementation workflow.
- `platform-v2-code-review`: code review, regression detection, and lessons learned.
- `platform-v2-verification-gate`: post-change verification.
- `platform-v2-commit-discipline`: smallest meaningful commits and pre-commit review.
- `platform-v2-dependency-upgrade`: dependency upgrades and open-source component adoption.

## 8. Verification Baseline

Backend main sources:

```bash
cd backend
mvn compile "-Dmaven.test.skip=true"
```

Backend test sources:

```bash
cd backend
mvn test-compile
```

Frontend:

```bash
cd frontend
npm run type-check
npm run build
```

## 9. Current Priority

1. Phase 0: restore compile, test-compile, and truthful CI gates.
2. Phase 1: stabilize Graph DSL v2 and validators.
3. Phase 2: build a Vue Flow designer POC without replacing the old designer immediately.
4. Phase 3: extract Runtime SPI.
5. Phase 4: unify Tool Plane.
6. Phase 5: add OpenTelemetry, metrics, logs, and CI gates.

