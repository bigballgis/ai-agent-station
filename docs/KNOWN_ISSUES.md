# Known Issues

> Keep this file factual. Do not use it as a wishlist. Every item should describe the current risk, evidence, and next action.

## KI-001: Backend Test Sources Are Not Yet a Reliable Gate

Status: Open (narrowed)  
Severity: Medium

Evidence:
- `mvn compile "-Dmaven.test.skip=true"` and `mvn test-compile` succeed on current main + test sources.
- `mvn test` still reports many failures: WebMvc slices needed extra `@MockBean`s (see `AbstractWebMvcSliceTest`), `GraphExecutor` constructor drift, repository/service API mismatches in older tests, and behavioral assertions that no longer match async / validation rules.
- Hypersistence JSON type imports and Spring Data `RedisCache*` package names were corrected; several missing Java imports and `AuditAction.READ` were restored so main sources compile cleanly.

Risk:
CI is only trustworthy once `mvn test` is green for the agreed scope; until then rely on compile + test-compile + targeted test packages.

Next action:
Run `mvn test`, group remaining failures by package (controller slice vs unit vs integration), and fix one category per task. Do not weaken production security to satisfy stale tests.

## KI-002: Gateway vs MVC Boundary Needs a Decision

Status: Open  
Severity: High

Evidence:
- The backend contains Spring Cloud Gateway-style filters and routes while also using a Spring MVC application with `/api` context path.
- Some route targets have historically used localhost-style routing.

Risk:
Deployment topology, security filters, and API gateway responsibilities may be unclear.

Next action:
Use `platform-v2-architecture-review` to decide whether to keep an in-process gateway pattern, remove it, or split a real Gateway service.

## KI-003: Flowise Dependency Decision Is Pending

Status: Open  
Severity: Medium

Evidence:
- Flowise packages have existed in frontend dependencies.
- Source usage must be verified before retaining them.

Risk:
Unused heavy dependencies increase install time, attack surface, and audit noise.

Next action:
Use `platform-v2-dependency-upgrade` to verify imports. Remove Flowise packages if unused, or integrate them through a narrow, documented POC if needed.

## KI-004: Documentation Language Was Historically Mixed

Status: In Progress  
Severity: Medium

Evidence:
- Core AI entry files and Skills have been converted to English.
- Some older docs and code comments may still contain Chinese.

Risk:
Lower-tier AI agents may produce mixed-language output or misunderstand repository conventions.

Next action:
Apply the "touch-and-convert" rule: when editing a file, convert nearby repository-facing Chinese prose/comments to English. Avoid unrelated mass translation.

## KI-005: Duplicate Deployment Document Casing Existed

Status: Mitigated  
Severity: Medium

Evidence:
- Both `docs/DEPLOYMENT.md` and `docs/deployment.md` existed historically.
- The intended quick reference is `docs/deployment.md`; full deployment guide is root `DEPLOYMENT.md`.

Risk:
Case-insensitive systems can confuse duplicate paths.

Next action:
Keep only the lowercase quick reference in active docs. Avoid adding new files that differ only by case.

## KI-006: Current Workflow Runtime Contains Technical Debt

Status: Open  
Severity: High

Evidence:
- Workflow execution logic has historically accumulated scheduling, node execution, tool invocation, checkpoint, and special-case behavior in a small number of classes.

Risk:
Runtime changes can become unsafe, hard to test, and hard to observe.

Next action:
Do not rewrite in one pass. Use Phase 3 of `PLATFORM_V2_EXECUTION_PLAN.md` to introduce Runtime SPI, then migrate one path at a time.

## KI-007: Claims Must Be Re-Verified

Status: Open  
Severity: Medium

Evidence:
- Old Round-based documentation contained large static claims about tests, metrics, and completion status.

Risk:
AI agents may treat historical claims as current truth.

Next action:
Trust code and verification commands first, `AGENTS.md` second, current docs third, historical changelog last.

