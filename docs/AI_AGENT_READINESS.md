# AI Agent Readiness

> This document explains the repository-level AI workflow: `AGENTS.md` as the single source of truth, a short `CLAUDE.md`, project Skills, and a v2 Core execution plan.

## 1. Key Decisions

- `CLAUDE.md` stays short and points to `AGENTS.md`.
- `AGENTS.md` is the cross-agent entry point.
- Complex workflows live in Skills, not in long global rules.
- Task plans must be executable, verifiable, and reversible.
- Formatting and style should be enforced by deterministic tools where possible.
- Repository language is English; owner-agent chat remains Simplified Chinese.

## 2. Current Structure

```text
AGENTS.md                          # unified AI agent entry
CLAUDE.md                          # Claude-specific short entry
.cursor/skills/*/SKILL.md          # project workflow Skills
docs/START_HERE.md                 # implementation startup entry
docs/PLATFORM_V2_EXECUTION_PLAN.md # v2 Core roadmap
docs/DECISIONS.md                  # architecture decisions
docs/KNOWN_ISSUES.md               # current blockers and known risks
docs/LESSONS_LEARNED.md            # recurring mistakes and prevention rules
docs/README.md                     # documentation index
```

## 3. Why Old Documents Were Removed

The old documentation set caused execution risk:

- Round-based logs made stale status look current.
- Multiple specs, task lists, and checklists conflicted with the v2 Core plan.
- Duplicate deployment documents created confusing entry points.
- Some documents described target state instead of verified current state.

The cleaned structure reduces context noise and gives lower-tier AI agents a smaller, more accurate starting point.

## 4. Recommended AI Prompts

### Restore Compile Baseline

```text
Read AGENTS.md first. Use platform-v2-startup-check, platform-v2-execution-plan, and platform-v2-verification-gate.
Execute Phase 0 Task 1: restore the compile baseline. Handle one failure category at a time.
```

### Vue Flow Designer POC

```text
Read AGENTS.md and docs/PLATFORM_V2_EXECUTION_PLAN.md.
Use platform-v2-safe-implementation and platform-v2-dependency-upgrade.
Build only a Vue Flow POC: add WorkflowDesignerV2, do not replace the old designer, and do not change the backend GraphDefinition contract.
```

### Tool Plane Upgrade

```text
Read AGENTS.md.
Use platform-v2-architecture-review, platform-v2-architecture-stewardship, and platform-v2-safe-implementation.
Unify Tool Plane without changing external business APIs. Start with ToolDescriptor, ToolInvocation, and ToolResult, then adapt MCP and HTTP tools.
```

### Pre-Commit Review

```text
Use platform-v2-code-review and platform-v2-commit-discipline.
Review the exact diff, split unrelated changes, run the relevant verification, and prepare the smallest meaningful commit.
```

## 5. Skill Maintenance Rules

- Skill names must be specific and task-triggerable.
- `SKILL.md` files should stay focused; long references belong in docs or scripts.
- Project Skills should contain project-specific workflow knowledge.
- General reusable Skills can later move to a global user-level Skills directory.
- Skills must not conflict with `AGENTS.md`; if they do, `AGENTS.md` wins.

