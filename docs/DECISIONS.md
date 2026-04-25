# Architecture Decisions

> This file records durable decisions that should guide future implementation. Add a new entry when a decision changes architecture, dependencies, data contracts, or AI workflow.

## Decision Format

```markdown
## ADR-000: Title

Status: Proposed | Accepted | Superseded
Date: YYYY-MM-DD

Context:
Decision:
Consequences:
Verification:
```

## ADR-001: Do Not Rewrite the Whole Platform

Status: Accepted  
Date: 2026-04-26

Context:
The project already contains useful product assets: multi-tenancy, RBAC, audit, API management, pages, deployment assets, and workflow/tooling foundations. A full rewrite would discard domain knowledge and increase delivery risk.

Decision:
Do not restart from zero. Execute a v2 Core refactor: preserve product assets while rebuilding fragile foundations.

Consequences:
- Existing product modules remain valuable.
- Refactoring must use adapters, feature flags, and small commits.
- Messy core code can still be replaced when it blocks safe evolution.

Verification:
Tasks must show the preserved contract and the new boundary being introduced.

## ADR-002: Repository Language Is English

Status: Accepted  
Date: 2026-04-26

Context:
Mixed Chinese/English documentation and comments make lower-tier AI agents less predictable and make repository collaboration harder.

Decision:
Repository content uses English: documentation, comments, Skills, commit messages, PR descriptions, and configuration comments. Owner-agent chat remains Simplified Chinese unless requested otherwise.

Consequences:
- New repository-facing prose must be English.
- Touched Chinese comments/docs should be converted to English in the same area.
- Do not mass-translate unrelated files without an explicit task.

Verification:
Review touched files for non-English repository-facing prose.

## ADR-003: v2 Core Plane Boundaries

Status: Accepted  
Date: 2026-04-26

Context:
The platform needs to evolve for workflow editing, workflow execution, tool invocation, observability, and enterprise governance without turning every module into a large coupled class.

Decision:
Use four conceptual planes:
- Control Plane: management, permissions, audit, versions, lifecycle.
- Runtime Plane: scheduling, execution, state, checkpoint, events, tool invocation.
- Designer Plane: visual editing and Graph DSL adaptation.
- Tool Plane: MCP / HTTP / Function / LLM tool descriptors, invocation, audit, and normalized results.

Consequences:
- New code should fit one plane clearly.
- Cross-plane calls should go through interfaces or adapters.
- Architecture review should block changes that mix these responsibilities.

Verification:
Use `platform-v2-architecture-stewardship` for boundary-sensitive changes.

## ADR-004: Vue Flow Is a Canvas Foundation, Not the Product Model

Status: Accepted  
Date: 2026-04-26

Context:
The current custom workflow canvas carries interaction and maintenance cost. A mature canvas library can reduce risk, but the platform must keep its own domain model.

Decision:
Use Vue Flow as a candidate canvas foundation in a POC. Keep the platform Graph DSL, node semantics, validation rules, permissions, versioning, and backend contracts independent.

Consequences:
- Build `WorkflowDesignerV2` behind a feature flag.
- Add adapters between Graph DSL and Vue Flow nodes/edges.
- Do not let Vue Flow become the persisted business model.

Verification:
Existing workflow definitions must open, edit, save, and execute through compatible DSL output.

## ADR-005: MCP Remains, but Through a Unified Tool Plane

Status: Accepted  
Date: 2026-04-26

Context:
MCP is a useful protocol for tool integration, but the platform must not depend on one tool protocol for every capability.

Decision:
Keep MCP support. Route MCP, HTTP, Internal Function, and future tool types through a unified Tool Plane.

Consequences:
- Runtime and LLM tool calling should depend on `ToolDescriptor`, `ToolInvocation`, and `ToolResult`, not MCP-specific classes.
- MCP protocol version and transport details remain configurable and isolated.
- Tool invocation must always include schema validation, tenant isolation, audit, timeout, and error classification.

Verification:
MCP and HTTP tools can be invoked through the same interface without losing audit fields.

