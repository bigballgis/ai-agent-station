---
name: platform-v2-architecture-review
description: Reviews AegisNexus architecture for v2 readiness. Use when auditing workflow runtime, designer, MCP/tooling, multi-tenancy, security, observability, or deciding whether to refactor or replace a module.
---

# Platform v2 Architecture Review

## Review Stance

Prioritize correctness, security, tenant isolation, evolvability, and operational safety. Do not praise surface completeness if core flows are stubs or unverified.

## Required Context

Read relevant files before judging:

- `AGENTS.md`
- `docs/PLATFORM_V2_EXECUTION_PLAN.md`
- `docs/architecture.md`
- Backend runtime/workflow files under `backend/src/main/java/com/aiagent/engine` and future `runtime`
- Designer files under `frontend/src/composables/designer` and `frontend/src/components/designer`
- Tool files under `backend/src/main/java/com/aiagent/mcp` and tool bridge packages

## Review Checklist

- Control Plane and Runtime Plane responsibilities are separated.
- Graph DSL is versioned and validated before save/execute.
- Runtime supports timeout, retry, parallelism, checkpoint, pause/resume, and event publishing.
- Tool calls have schema validation, tenant isolation, permissions, timeout, audit, and error classification.
- Designer uses a stable adapter between visual state and persisted DSL.
- Public API contracts remain stable.
- Data migrations are Flyway-based and reversible by operational procedure.
- Observability includes traceId, tenantId, userId, workflowId, executionId.
- CI contains real gates, not fake artifacts.

## Output Format

Start with findings ordered by severity:

```markdown
## Findings
- Critical: ...
- High: ...
- Medium: ...

## Required Refactor
...

## Safe Migration Path
...

## Verification Gates
...
```

## Decision Rule

Recommend full rewrite only if the current module cannot be adapted without breaking core contracts. Otherwise recommend strangler-style refactor with adapters and feature flags.

