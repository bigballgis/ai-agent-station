# Documentation Index

> This directory keeps only current, actionable documentation. Historical Round logs, obsolete task lists, and duplicate deployment entry points are intentionally removed to prevent AI agents from following stale instructions.

## Current Authoritative Documents

- `../AGENTS.md`: unified AI coding agent entry point.
- `START_HERE.md`: first file to read before implementation work.
- `PLATFORM_V2_EXECUTION_PLAN.md`: v2 Core roadmap and task packages.
- `DECISIONS.md`: durable architecture decisions.
- `KNOWN_ISSUES.md`: current blockers and known risks.
- `LESSONS_LEARNED.md`: recurring mistakes and prevention rules.
- `architecture.md`: current architecture and target architecture notes.
- `security.md`: security baseline and continuous verification requirements.
- `deployment.md`: deployment quick reference.
- `../DEPLOYMENT.md`: complete deployment guide.
- `../backend/src/main/resources/api-changelog.md`: API list and changelog.

## Documentation Rules

- Do not add new Round-style progress logs.
- Do not maintain multiple deployment guides with overlapping content.
- Task-oriented content belongs in `PLATFORM_V2_EXECUTION_PLAN.md` or a Cursor Skill.
- Historical status must not be written as current completion status.
- If documentation conflicts with code, verify the code first and update the documentation.
- Repository documentation must be written in English.

## AI Reading Order

1. `../AGENTS.md`
2. A matching Skill under `../.cursor/skills/*/SKILL.md`
3. `START_HERE.md`
4. `PLATFORM_V2_EXECUTION_PLAN.md`
5. Task-specific architecture, security, deployment, or API documents

