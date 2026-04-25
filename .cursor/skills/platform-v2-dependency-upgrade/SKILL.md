---
name: platform-v2-dependency-upgrade
description: Manages dependency upgrades and open-source component adoption for AegisNexus. Use when adding Vue Flow, MCP SDKs, LangChain4j upgrades, Spring Boot upgrades, dependency cleanup, or security update work.
---

# Platform v2 Dependency Upgrade

## Decision Rules

Read `AGENTS.md` before adding, upgrading, or removing dependencies.

Add or upgrade a dependency only if:

- it is used immediately in production code or a committed POC behind a feature flag
- it has a clear owner in the architecture
- it improves maintainability, security, performance, or standards compatibility
- it does not duplicate an existing library without a migration plan

Remove a dependency if:

- no source code imports it
- it only exists in lockfiles
- it drags heavy transitive packages without business use
- it expands security scan scope without benefit

## Preferred Components

- Visual workflow canvas: prefer `@vue-flow/core` for Vue 3 compatibility.
- Auto layout: prefer `dagre` for simple DAG; use `elkjs` for complex layouts.
- Tool protocol: prefer MCP standard and official/community SDK only after license and Spring compatibility check.
- Observability: prefer OpenTelemetry, Micrometer, Prometheus.
- Contracts: prefer OpenAPI and JSON Schema.

## Upgrade Workflow

1. Search current usage.
2. Confirm license and maintenance status.
3. Add one dependency group at a time.
4. Use it in a narrow path immediately.
5. Run type-check/build or backend compile.
6. Document why it was added.

## Package Rules

- Do not keep Flowise packages unless the UI or SDK is explicitly integrated.
- Do not mix React-only editor libraries into the Vue app unless the whole frontend migration is approved.
- Do not add beta/experimental packages to core runtime without an isolation layer.

## Output Format

```markdown
Dependency decision:
Purpose:
Alternatives:
Risks:
Verification:
Rollback:
```

