---
name: platform-v2-execution-plan
description: Executes the AegisNexus v2 Core migration plan step by step. Use when implementing the platform v2 roadmap, splitting work into safe tasks, or asking an AI agent to continue the architecture refactor.
---

# Platform v2 Execution Plan

## Mandatory First Steps

1. Read `AGENTS.md`.
2. Read `docs/PLATFORM_V2_EXECUTION_PLAN.md`.
3. Identify the current phase: Phase 0, 1, 2, 3, 4, or 5.
4. State the exact task package being executed.
5. Do not modify files outside the task scope.

## Task Template

Before coding, write this in the response:

```markdown
Task:
Goal:
Allowed files:
Forbidden changes:
Required reading:
Verification commands:
Risks:
```

## Execution Rules

- Keep each change small and reversible.
- Prefer adapters over breaking old contracts.
- Preserve existing API response shape: `{ code, message, data }`.
- Preserve backend 0-based pagination and frontend 1-based pagination conversion.
- Do not rewrite the whole project.
- Do not change database schema without adding a Flyway migration.
- Do not remove existing user changes.

## Phase Order

1. Restore compile and test baseline.
2. Stabilize Graph DSL v2 and validator.
3. Introduce Vue Flow designer behind a feature flag.
4. Extract runtime SPI from `GraphExecutor`.
5. Unify Tool Plane across MCP, HTTP, and Function tools.
6. Add observability and CI verification gates.

## Verification

Run the narrowest reliable checks first:

```bash
cd backend && mvn compile "-Dmaven.test.skip=true"
cd frontend && npm run type-check
```

If touching tests, also run:

```bash
cd backend && mvn test-compile
cd frontend && npm run test:run
```

## Completion Output

End with:

- Files changed
- Behavior changed
- Verification run
- Remaining risks
- Next recommended task

