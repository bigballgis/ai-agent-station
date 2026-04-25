---
name: platform-v2-safe-implementation
description: Implements AegisNexus v2 changes safely. Use when coding refactors for Graph DSL, Vue Flow designer, runtime SPI, MCP/tool plane, CI gates, or dependency cleanup.
---

# Platform v2 Safe Implementation

## Before Editing

1. Read `AGENTS.md`.
2. Read the task and identify allowed files.
3. Read current implementations before modifying them.
4. Check for user changes and do not revert unrelated edits.
5. Prefer adding adapters and new modules before replacing existing modules.

## File Scope Rules

- Designer refactor: limit to `frontend/src/pages/WorkflowDesigner*`, `frontend/src/composables/designer`, `frontend/src/components/designer`, and package files if adding a dependency.
- Runtime refactor: limit to `backend/src/main/java/com/aiagent/runtime`, then adapt old `engine/graph` classes narrowly.
- Tool Plane refactor: limit to `backend/src/main/java/com/aiagent/mcp`, tool bridges, and new `runtime/tool` package.
- Schema changes require Flyway migration.
- API changes require frontend API client and docs update.

## Coding Rules

- Preserve existing response wrappers and pagination conventions.
- Keep tenantId explicit in queries and execution context.
- Do not log secrets, tokens, API keys, tool arguments containing credentials, or raw LLM prompts unless explicitly masked.
- Do not add broad fallback logic that hides data corruption.
- Do not add new dependencies unless they are used immediately and justified.
- Prefer typed DTOs over `Map<String, Object>` at boundaries; inside dynamic node config, validate with JSON Schema.

## Refactor Pattern

Use this order:

1. Add new interface/type.
2. Add adapter from old model to new model.
3. Add tests for adapter/validator.
4. Route one narrow flow through the new path.
5. Keep old path behind feature flag until parity is verified.
6. Remove old path only in a separate cleanup task.

## Verification

After edits, run lints/compilation for touched areas. Minimum:

```bash
cd backend && mvn compile "-Dmaven.test.skip=true"
cd frontend && npm run type-check
```

If verification cannot run, state the exact reason and what remains unverified.

