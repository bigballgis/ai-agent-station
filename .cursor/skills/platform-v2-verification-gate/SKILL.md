---
name: platform-v2-verification-gate
description: Verifies AegisNexus changes before declaring completion. Use after edits, before commits, after refactors, or when diagnosing failed CI/test/build results.
---

# Platform v2 Verification Gate

## Principle

Never declare completion from code inspection alone. Verify the smallest useful scope, then widen.
Read `AGENTS.md` first if the task touches platform architecture, runtime, tools, security, CI, or documentation.

## Backend Gates

Run in order:

```bash
cd backend
mvn compile "-Dmaven.test.skip=true"
mvn test-compile
mvn test
```

If `test-compile` fails from unrelated stale tests, report:

- first 5 failing test files
- whether main compile passed
- whether production code is affected
- recommended repair task

## Frontend Gates

Run in order:

```bash
cd frontend
npm run type-check
npm run test:run
npm run build
```

If changing designer dependencies, also inspect bundle size:

```bash
cd frontend
npm run ci:bundle-check
```

## Security Gates

Check manually for:

- no secrets in code or config
- no raw token/API key logging
- no SSRF bypass
- no tenantId omission in tenant-scoped queries
- no unsafe dynamic script execution

## Completion Format

```markdown
## Verification
- Backend compile: pass/fail/not run
- Backend test-compile: pass/fail/not run
- Backend tests: pass/fail/not run
- Frontend type-check: pass/fail/not run
- Frontend build: pass/fail/not run

## Blockers
...

## Residual Risk
...
```

