# AegisNexus v2 Core Execution Plan

> Goal: keep useful product assets while rebuilding the core platform foundations: designer, Graph DSL, runtime, Tool Plane, verification gates, and dependency governance.

## 0. Principles

- **Do not rewrite the whole product from scratch.** Preserve multi-tenancy, RBAC, audit, API management, pages, deployment assets, and useful domain code.
- **Rebuild the core where it matters.** Replace fragile canvas logic, overloaded workflow execution, scattered tool invocation, and fake quality gates.
- **Protocol-first architecture.** Prefer OpenAPI, JSON Schema, MCP, OpenTelemetry, Prometheus, and explicit event contracts.
- **Small reversible steps.** Use adapters, feature flags, compatibility layers, and focused commits.
- **AI-executable tasks.** Every task must define scope, forbidden changes, required reading, verification commands, and failure handling.

## 1. Target Architecture

### 1.1 Control Plane

Owns management concerns:

- Agent, Workflow, Tool, Tenant, User, Role, and Permission management
- Versioning, approval, release, rollback
- Quotas, audit logs, import/export
- API documentation, operations configuration, dashboards

Keep it in the existing Spring Boot application for now, but clean boundaries so it can evolve safely.

### 1.2 Runtime Plane

Owns execution concerns:

- Graph DSL validation
- Workflow scheduling
- Node execution
- Parallelism, timeout, retry, compensation
- Checkpoint and resume
- Human approval pause/resume
- Tool invocation and normalized results
- Runtime event publishing

Short term: same Spring Boot process. Long term: separable runtime service.

### 1.3 Designer Plane

Owns visual workflow editing:

- Canvas, drag/drop, connections, zoom, minimap
- Node configuration panel
- Validation hints
- Import/export

Preferred direction: use `@vue-flow/core` as the canvas foundation while preserving the platform Graph DSL and node semantics.

### 1.4 Tool Plane

Unify tool types behind one contract:

- MCP Tool
- HTTP Tool
- Internal Function Tool
- LLM Tool
- Future Script/Sandbox Tool

Every tool call must enforce schema validation, tenant isolation, permissions, timeout, audit, result normalization, and error classification.

## 2. Roadmap

### Phase 0: Baseline Recovery

Goal: make the project safe to refactor.

Tasks:

1. Restore backend main-source compile.
2. Restore or explicitly isolate stale test compilation failures.
3. Remove unused heavy dependencies.
4. Replace fake CI artifacts with real gates.
5. Freeze a truthful capability list: implemented, partial, placeholder, planned.

Acceptance:

- `backend` main sources compile.
- `frontend` type-check passes.
- Known stale tests are listed as repair tasks.
- CI does not publish missing or fake reports.

### Phase 1: Graph DSL v2

Goal: define what a workflow graph is before replacing designer or runtime.

Define:

- `GraphDefinition`
- `NodeDefinition`
- `EdgeDefinition`
- variables
- triggers
- metadata
- retry and timeout policies

Validator requirements:

- unique node IDs
- valid edge endpoints
- allowed graph shape
- no illegal cycles
- start/end rules
- known node types
- schema compatibility

Acceptance:

- Old workflow definitions can still be read through an adapter.
- New DSL has validator tests.
- Backend validates before save/execute.
- Frontend serialization matches backend contract.

### Phase 2: Vue Flow Designer POC

Goal: replace the fragile canvas foundation without breaking persisted workflow data.

Tasks:

1. Add `WorkflowDesignerV2.vue`.
2. Add adapter: `GraphDefinition -> Vue Flow nodes/edges`.
3. Add adapter: Vue Flow changes -> `GraphDefinition`.
4. Port node rendering as Vue Flow custom nodes.
5. Use Vue Flow handles for connections.
6. Use `dagre` or `elkjs` for auto-layout if needed.
7. Keep old designer behind a feature flag until parity is proven.

Acceptance:

- Existing workflow opens correctly.
- User can add, move, connect, disconnect, and delete nodes.
- Save output remains compatible.
- Large graph interaction is acceptable.

### Phase 3: Runtime Core Refactor

Goal: split overloaded execution logic into testable runtime modules.

Target package:

```text
backend/src/main/java/com/aiagent/runtime/
  dsl/
  validation/
  scheduler/
  executor/
  node/
  state/
  checkpoint/
  event/
  tool/
```

Core interfaces:

```java
GraphValidator
WorkflowScheduler
WorkflowRuntime
NodeExecutor
ExecutionStateStore
CheckpointStore
RuntimeEventPublisher
ToolInvoker
```

Acceptance:

- Serial DAG, parallel DAG, retry, timeout, pause/resume, and failure paths are tested.
- Node execution logic is not embedded in the scheduler loop.
- Runtime can be tested without controllers.

### Phase 4: Unified Tool Plane

Goal: make MCP, HTTP, and Function tools interchangeable to the runtime and LLM layer.

Core types:

- `ToolDescriptor`
- `ToolInvocation`
- `ToolResult`
- `ToolError`
- `ToolProvider`
- `ToolInvoker`

MCP requirements:

- configurable `protocolVersion`
- `initialize`
- `notifications/initialized`
- `tools/list`
- `tools/call`
- future Streamable HTTP support

Acceptance:

- Runtime does not care whether the tool source is MCP, HTTP, or Function.
- Tool invocation audit fields remain complete.
- Tool failures do not leak secrets.

### Phase 5: Observability and Quality Gates

Goal: make the platform operable and safe to evolve.

Add or verify:

- OpenTelemetry trace around workflow, node, and tool execution
- Prometheus metrics for duration, error rate, queue depth, tool latency
- structured logs with `traceId`, `tenantId`, `userId`, `workflowId`, `executionId`
- CI gates for backend compile, test compile, tests, frontend type-check, frontend tests, build, and dependency scan

Acceptance:

- A workflow execution can be traced from HTTP request to node/tool execution.
- CI failure reasons are actionable.
- No fake green gates.

## 3. Standard AI Task Template

Every implementation task must start with:

```markdown
Task:
Goal:
Allowed files:
Forbidden changes:
Required reading:
Steps:
Verification commands:
Failure handling:
Completion output:
```

## 4. First Task Packages

### Task 1: Restore Compile Baseline

Allowed files:

- `backend/pom.xml`
- obvious syntax error files
- stale tests and mocks

Verification:

```bash
cd backend
mvn compile "-Dmaven.test.skip=true"
mvn test-compile
```

### Task 2: Add Graph DSL v2 Types and Validator

Allowed files:

- `backend/src/main/java/com/aiagent/runtime/dsl`
- `backend/src/main/java/com/aiagent/runtime/validation`
- related tests
- `frontend/src/composables/designer/types.ts`

Acceptance:

- v1/v2 adapter tests pass.
- validator covers at least core invalid graph cases.

### Task 3: Vue Flow POC

Allowed files:

- `frontend/package.json`
- `frontend/src/pages/WorkflowDesignerV2.vue`
- `frontend/src/composables/designer/vueFlowAdapter.ts`
- route/feature flag wiring

Verification:

```bash
cd frontend
npm run type-check
npm run build
```

### Task 4: Runtime SPI

Allowed files:

- `backend/src/main/java/com/aiagent/runtime/**`
- narrow adapters in old `engine/graph` classes

Acceptance:

- old API remains stable
- new SPI tests cover serial, parallel, and failed execution

### Task 5: Tool Plane Unification

Allowed files:

- `backend/src/main/java/com/aiagent/runtime/tool/**`
- `backend/src/main/java/com/aiagent/mcp/**`
- tool bridge packages

Acceptance:

- MCP and HTTP tools go through a unified interface.
- audit and error classification remain intact.

## 5. Final Direction

The project should not restart from zero. The correct path is:

> Preserve product assets, rebuild v2 Core, replace fragile foundations with mature open-source components, and verify every step.

