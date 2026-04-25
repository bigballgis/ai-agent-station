# Lessons Learned

> Record recurring mistakes here so future AI agents do not repeat them. Keep entries short, factual, and tied to prevention rules.

## LL-001: Do Not Treat Historical Documentation as Current Truth

Mistake:
Round-based docs claimed completion status that was not always verified against current code.

Impact:
AI agents could start from a false baseline.

Prevention:
Use code and commands as truth. `AGENTS.md` defines current rules; historical changelog is only history.

Related rule:
`AGENTS.md` → Non-Negotiable Engineering Rules and AI Execution Rules.

## LL-002: Dependency Coordinates Must Be Verified

Mistake:
A dependency used an outdated Maven group ID and blocked compilation.

Impact:
Compile verification failed before real code issues could be evaluated.

Prevention:
When dependency resolution fails, verify current coordinates from Maven Central or official docs before changing code around the failure.

Related Skill:
`platform-v2-dependency-upgrade`

## LL-003: Test Debt Must Not Drive Unsafe Production Changes

Mistake:
Stale tests can fail because constructors, repositories, or services changed, not because production code is wrong.

Impact:
There is a temptation to weaken production code to satisfy old tests.

Prevention:
Separate main-source compile from test-source compile. Repair tests in dedicated tasks and preserve production safety.

Related Skill:
`platform-v2-verification-gate`

## LL-004: Mixed Repository Language Reduces Agent Reliability

Mistake:
Repository-facing docs and comments used mixed Chinese and English.

Impact:
Lower-tier AI agents may generate inconsistent docs, comments, commits, and plans.

Prevention:
Repository language is English. Owner-agent chat remains Simplified Chinese.

Related rule:
`AGENTS.md` → Language Policy.

## LL-005: Large Refactors Need Boundaries Before Movement

Mistake:
Complex classes can invite broad rewrites before stable interfaces exist.

Impact:
Large diffs become hard to review and hard to roll back.

Prevention:
Introduce interfaces, adapters, and feature flags first. Move one flow at a time.

Related Skill:
`platform-v2-architecture-stewardship`

## LL-006: Small Commits Are a Safety Mechanism

Mistake:
Combining dependency fixes, syntax fixes, docs cleanup, and architecture changes into one commit hides risk.

Impact:
Review, rollback, and blame become harder.

Prevention:
Use the smallest meaningful commit. Separate unrelated changes.

Related Skill:
`platform-v2-commit-discipline`

