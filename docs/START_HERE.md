# Start Here

> Use this file before starting any implementation work. The goal is to prevent unfocused edits, stale-document driven changes, and unverifiable refactors.

## Current Operating Mode

The project is in **v2 Core preparation**.

Do not start with large feature work. The first priority is to restore a trustworthy engineering baseline:

1. Backend main-source compile.
2. Backend test-source compile.
3. Frontend type-check and build.
4. CI gates that reflect real verification.
5. A clear list of known issues and architectural decisions.

## Required Reading Order

1. `../AGENTS.md`
2. `README.md` in this directory
3. `PLATFORM_V2_EXECUTION_PLAN.md`
4. `KNOWN_ISSUES.md`
5. `DECISIONS.md`
6. The project Skill that matches the task

## First Recommended Task

Use `platform-v2-startup-check` and then execute:

```text
Phase 0 Task 1: restore the compile baseline.
Handle one failure category at a time.
Do not modify designer, runtime architecture, database schema, or CI in the same task.
```

## Before Any AI Starts Coding

The AI must report:

```markdown
Current phase:
Task:
Allowed files:
Forbidden changes:
Required reading completed:
Verification commands:
Known blockers:
```

## Do Not Start With

- Replacing the workflow designer.
- Rewriting `GraphExecutor`.
- Adding new dependencies without a narrow POC.
- Changing database schema.
- Reworking CI before the local baseline is understood.
- Translating the entire repository in one pass.

## Safe First Changes

- Fix one compile error category.
- Fix stale tests after main-source compile is green.
- Convert touched comments/docs to English.
- Remove or document unused dependencies.
- Add narrow adapters before moving large code.

## Definition of Done for Startup Work

- The task scope is explicit.
- The changed files are limited and reviewable.
- The verification result is recorded.
- Any unresolved blocker is documented in `KNOWN_ISSUES.md`.
- Any long-term architecture decision is documented in `DECISIONS.md`.

