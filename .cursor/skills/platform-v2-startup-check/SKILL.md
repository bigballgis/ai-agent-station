---
name: platform-v2-startup-check
description: Performs the required startup assessment before AegisNexus work begins. Use at the beginning of a new session, before implementation, or when an AI agent needs to decide the next safe task.
---

# Platform v2 Startup Check

## Purpose

Prevent premature implementation. Establish the current phase, known blockers, safe scope, and verification plan before any code changes.

## Required Reading

1. `AGENTS.md`
2. `docs/START_HERE.md`
3. `docs/KNOWN_ISSUES.md`
4. `docs/DECISIONS.md`
5. The task-specific Skill, if any

## Startup Procedure

1. Check the user's latest request.
2. Identify the phase from `docs/PLATFORM_V2_EXECUTION_PLAN.md`.
3. Inspect `git status` before editing.
4. Identify files that are already modified.
5. Determine whether the task is planning, review, implementation, verification, or commit preparation.
6. Pick the matching Skill:
   - planning: `platform-v2-execution-plan`
   - architecture: `platform-v2-architecture-stewardship`
   - implementation: `platform-v2-safe-implementation`
   - review: `platform-v2-code-review`
   - verification: `platform-v2-verification-gate`
   - commit: `platform-v2-commit-discipline`
   - dependencies: `platform-v2-dependency-upgrade`

## Required Output Before Work

```markdown
Current phase:
Request type:
Relevant Skill:
Allowed files:
Forbidden changes:
Existing dirty files:
Known blockers:
Verification commands:
Recommended next step:
```

## Stop Conditions

Stop and ask for direction when:

- the user requests broad work without a clear priority
- existing dirty files overlap with the intended change and ownership is unclear
- the task would mix runtime, designer, database, and CI changes
- a required dependency or external service cannot be verified

## Default Next Task

If no specific implementation task is provided, recommend:

```text
Phase 0 Task 1: restore the compile baseline, one failure category at a time.
```

