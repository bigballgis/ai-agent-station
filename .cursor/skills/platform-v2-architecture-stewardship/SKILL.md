---
name: platform-v2-architecture-stewardship
description: Maintains global architecture direction for AegisNexus and authorizes scoped technical-debt cleanup. Use when refactoring messy code, reducing architectural drift, deciding module boundaries, or planning v2 Core work.
---

# Platform v2 Architecture Stewardship

## Mission

Keep the platform evolvable for the next two years. Preserve useful product assets, but do not protect messy code that blocks safe change. Refactor with courage and discipline.

## Required Reading

1. `AGENTS.md`
2. `docs/PLATFORM_V2_EXECUTION_PLAN.md`
3. The affected module and its direct callers

## Architectural North Star

- Control Plane owns management, permissions, audit, versioning, and lifecycle.
- Runtime Plane owns scheduling, execution, state, checkpoint, events, and tool invocation.
- Designer Plane owns visual editing and adapts to Graph DSL.
- Tool Plane owns MCP / HTTP / Function tool descriptions, invocation, audit, and normalized results.

## When to Refactor

Refactor immediately when code:

- mixes orchestration, persistence, UI contracts, and protocol logic in one class
- hides failures through broad fallbacks
- weakens tenant isolation, audit, or security
- makes tests impossible without unsafe production changes
- duplicates behavior across modules
- blocks the v2 Core migration path

## Refactor Constraints

- Keep one refactor theme per task.
- Add boundaries before moving large amounts of code.
- Prefer adapters and feature flags for risky migrations.
- Preserve external contracts unless the task explicitly changes them.
- Leave a verification trail.

## Output Format

```markdown
Architectural issue:
Why it blocks v2 Core:
Target boundary:
Minimal refactor step:
Files allowed:
Verification:
Follow-up tasks:
```

