---
name: platform-v2-code-review
description: Reviews AegisNexus code changes with architectural awareness, security focus, and lessons-learned tracking. Use before declaring complex work complete, when reviewing diffs, or when the user asks for code review.
---

# Platform v2 Code Review

## Review Posture

Act as a senior reviewer. Findings come first. Focus on correctness, security, tenant isolation, maintainability, runtime behavior, and missing verification. Do not spend review budget on style nits that tools can catch.

## Required Reading

1. `AGENTS.md`
2. The task description and changed files
3. Relevant upstream/downstream callers
4. Relevant tests, if present

## Review Checklist

- The change preserves API response and pagination contracts.
- Tenant isolation, permissions, audit, and secret masking are not weakened.
- Runtime, workflow, MCP/tooling, and designer changes respect the v2 Core boundaries.
- No new unused dependency, broad fallback, or silent data corruption path is introduced.
- Error handling is explicit and observable.
- Tests or verification commands match the risk level.
- Documentation does not claim future-state work is already complete.
- Repository-facing prose and comments are English.

## Lessons Learned

When a recurring mistake is found, add a short "Lessons learned" section to the review output:

```markdown
## Lessons Learned
- Mistake: ...
- Root cause: ...
- Rule to prevent recurrence: ...
- Suggested Skill/AGENTS.md update: ...
```

Only update `AGENTS.md` or Skills when the rule is broadly reusable.

## Output Format

```markdown
## Findings
- Critical: ...
- High: ...
- Medium: ...

## Verification Gaps
- ...

## Lessons Learned
- ...

## Review Verdict
Block / Approve with follow-up / Approve
```

