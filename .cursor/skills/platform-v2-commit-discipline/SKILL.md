---
name: platform-v2-commit-discipline
description: Prepares smallest meaningful commits for AegisNexus with pre-commit review and verification. Use when the user asks to commit, split changes, prepare commits, or review changes before committing.
---

# Platform v2 Commit Discipline

## Purpose

Create small, reviewable commits that preserve project safety. Never commit unless the user explicitly asks for a commit.

## Before Committing

1. Read `AGENTS.md`.
2. Inspect `git status`.
3. Inspect staged and unstaged diffs.
4. Separate unrelated changes into different commits.
5. Run `platform-v2-code-review` mentally on the exact commit scope.
6. Run the narrowest useful verification from `platform-v2-verification-gate`.

## Commit Scope Rules

One commit should contain one reason:

- One bug fix
- One refactor step
- One dependency decision
- One documentation cleanup
- One test repair

Do not mix:

- Runtime refactor + designer changes
- Dependency upgrade + unrelated cleanup
- Formatting-only churn + behavior change
- Test rewrites + production architecture changes

## Pre-Commit Review

Before creating a commit, report:

```markdown
Commit scope:
Files included:
Files intentionally excluded:
Verification:
Risks:
Commit message:
```

## Commit Message Style

Use concise English messages:

```text
fix(mcp): configure protocol negotiation
refactor(runtime): introduce node executor boundary
docs(agent): add repository language policy
test(auth): align jwt utility constructor mocks
```

## Safety Rules

- Never include secrets, `.env`, credentials, or generated private files.
- Never amend or force-push unless the user explicitly asks and it is safe.
- Never stage unrelated user edits.
- If hooks modify files, re-check the diff before any follow-up commit.

