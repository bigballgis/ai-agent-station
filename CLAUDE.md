---
name: Financial-Grade AI Agent Orchestration Platform
description: 36h auto evolution, locked workflow & API, financial production level, no messy code, online latest best practices, self-refresh
version: 1.0
author: User
tags: [ai-agent, low-code, workflow, api-management, financial, evolution]
---

# 8 Permanent Iron Rules

1. **36-Hour Continuous Loop**: online research -> review -> fix -> upgrade -> compliance -> optimization.
2. **No-Code AI Agent Platform**: for non-IT business users. Full autonomy to business teams.
3. **Visual Workflow Orchestration**: permanent core. Only enhance, never drift or weaken.
4. **Unified API Gateway & Lifecycle Management**: permanent core.
5. **Financial Enterprise Production Standards**: security, audit, high availability, isolation.
6. **Zero Messy Code, Zero Technical Debt**: refactor bad code every round.
7. **Latest Industry Best Practices**: search and integrate every iteration.
8. **Full SKILL Output**: after each round, output full SKILL content to prevent context loss.

# Iteration Flow

1. Read 8 rules
2. Online research for latest standards
3. Full review: security, code, architecture, compliance, core functions
4. Fix, refactor, upgrade, enhance core modules
5. Verify production availability
6. Output full skill to refresh context
7. Start next loop automatically

# Termination Conditions

1. 36 hours completed
2. 10 clean rounds without high/medium risks
3. Workflow & API closed-loop ready
4. Financial compliance validated
5. Latest best practices integrated

# Project Architecture

## Tech Stack
- **Frontend**: Vue 3 + TypeScript + Vite + Ant Design Vue + Tailwind CSS + vue-i18n
- **Backend**: Spring Boot 3 + Java 17 + Spring Security + JWT + LangChain4j
- **Database**: PostgreSQL 16 (pgvector) + Redis 7 (cluster optional)
- **Migration**: Flyway
- **Monitoring**: Spring Boot Actuator + Prometheus + Grafana
- **Build**: Docker Compose (multi-container)
- **CI/CD**: GitHub Actions
- **Testing**: Vitest (unit) + Playwright (E2E, configured)

## Core Modules
- Agent Management (CRUD, versioning, templates, debugging)
- Visual Workflow Designer (drag-and-drop orchestration)
- API Gateway (unified management, lifecycle, documentation)
- MCP Tool Market (tool discovery and binding)
- Memory Management (short-term, long-term, business memory)
- Alert & Monitoring (rules, records, real-time notification)
- Testing (test cases, execution, results)
- Evolution Engine (self-optimization suggestions, experience data)
- Multi-Tenancy (tenant isolation, quota management)
- RBAC Permission System (roles, permissions, audit logs)

## Key Conventions
- Frontend i18n: vue-i18n with zh-CN/en-US, all user-facing strings must use t()
- Dark mode: Tailwind dark: prefix + :global(.dark) CSS overrides
- API responses: `{ code, message, data }` wrapper, PageResult uses `{ records, total, totalPages, page, size }`
- Auth: JWT access token + refresh token, stored via authStorage utility
- Pagination: backend 0-based, frontend 1-based (convert with page - 1)
- Error handling: request.ts interceptor handles 401/403/429/5xx globally

## Completed Iterations History

### Round 1 (GPT-5.4 Rectification)
- Security audit: rate limiting, Docker port binding, JWT issuer validation
- Architecture: Service layer extraction, transaction management
- UX: registration page, forgot password, notification bell, search

### Round 2 (User Journey Comprehensive Review)
- P0 fixes: dead links, missing pages, non-functional UI elements
- P1 fixes: i18n coverage, backend permissions, route unification
- P2 fixes: dashboard data, error reporting, responsive, a11y

### Round 3 (Deep Code Quality Audit)
- Flyway V22 migration for execution_history table
- ExecutionHistoryService layer extraction
- DELETE permission fix, DTO validation, @Transactional
- CORS dedup, dead code cleanup

### Round 4 (Deep UX Audit)
- P0: PageResult type mismatch fix (14 files), Dict Store path fix, X-Tenant-ID headers, copyAgent param fix
- P1: Token refresh mechanism, breadcrumb i18n, login redirect, captcha error handling
- P2: Public component i18n (5 components), core page i18n (6 pages, 150+ keys), dark mode (5 pages), form validation (3 pages)

### Round 5 (Final Polish)
- AgentCanvas i18n: 4 components, 100+ canvas namespace keys in zh-CN/en-US
- BrandPanel i18n: brand title and copyright text
- Backend structured logging: file output with date rolling, per-package log levels
- .env.production.example: security warning header
- NotFound.vue: dark mode verified complete
- ConfirmModal a11y: role="alertdialog" and aria-modal="true"
- ProTable a11y: aria-label on table
- Health Check: show-components enabled for DB/Redis visibility
- Vite gzip: checked, plugin not installed, skipped per requirements

### Round 6 (Stability Enhancement)
- Backend Thread.sleep audit: DeploymentService marked TODO for production replacement, Delay nodes (WorkflowEngine/NodeExecutors) confirmed business requirement with 1-300s timeout cap
- Frontend ErrorBoundary component: Vue error boundary wrapping router-view in App.vue, catches child component render errors with retry
- Frontend API retry: GET requests auto-retry up to 2 times with 1s delay on 5xx/network errors (POST/PUT/DELETE excluded)
- Backend RateLimit annotation + AOP aspect: method-level rate limiting with sliding window counter, IP-based per-endpoint throttling
- Frontend localStorage capacity management: storage.ts utility with 5MB limit, near-full detection, and cleanup mechanism
- Backend API response time monitoring: TraceFilter enhanced with X-Response-Time header, MDC responseTimeMs, slow request warning (>3s)
- Frontend route prefetch: Dashboard route annotated with webpackPrefetch for faster initial load
- Backend CORS: confirmed environment-variable-driven via CORS_ALLOWED_ORIGINS in application.yml
- Frontend i18n fallback: silentFallbackWarn/silentTranslationWarn enabled, missing key handler with dev-only warnings
- i18n keys added: common.renderError, common.retry in zh-CN/en-US

### Round 7 (Final Polish - Comprehensive)
- **i18n hardcode cleanup**: OptimizationSuggestion.vue fully i18n (template + script + mock data), LogCenter.vue log type comparison, McpToolMarket.vue placeholder, FileManagement.vue type labels, AgentTemplateMarket.vue defaults, WorkflowInstance.vue cancel reason, Login.vue/BrandPanel.vue fallback strings removed, MainLayout.vue language labels
- **i18n keys added**: optimizationSuggestion namespace (30+ keys), logCenter namespace (3 keys), mcpTool.inputApiKey/inputBearerToken/toggleToolFailed, tplMarket.defaultCategory/defaultCreator, workflow.userCancelReason
- **Backend log sanitization**: OperationLogAspect masks sensitive parameters (password, secret, token, apiKey, etc.) in audit logs; DataChangeAuditAspect skips sensitive fields in data change audit
- **Vite env types**: vite-env.d.ts enhanced with ImportMetaEnv interface for VITE_API_BASE_URL and VITE_APP_TITLE
- **application.yml optimization**: server.port via SERVER_PORT env var, spring.jpa.open-in-view=false, spring.jackson date-format/time-zone/write-dates-as-timestamps configured
- **package.json scripts**: lint extended to .js/.tsx, added type-check script (vue-tsc --noEmit)
- **PWA manifest**: Created public/manifest.json with basic PWA configuration
- **Backend README**: Created /backend/README.md with tech stack, local run guide, env vars, API docs link, project structure
- **Playwright E2E**: Created playwright.config.ts with chromium/firefox/webkit projects and dev server config
- **Docker healthcheck**: All services (postgres, redis, prometheus, grafana, backend, frontend) confirmed with healthcheck configuration

### Round 8-10 (Architecture & Type Safety)
- God Class split: GraphExecutor 1123 lines -> 4 classes (GraphExecutor, NodeExecutors, HttpExecutor, SsrfValidator)
- Memory pagination OOM fix: 9 endpoints -> DB pagination with JPQL
- N+1 query fix: UserDataService 6x findById -> 1 call, ExperienceServiceImpl -> DB aggregation
- 50+ `as any` -> proper TypeScript types
- 3 exception subclasses: ResourceNotFoundException(404), RateLimitException(429), AuthenticationException(401)
- 18 permission annotations, 22 @Valid annotations added
- Vite manualChunks: all chunks < 520KB

### Round 11-20 (Security & Quality Hardening)
- MDC traceId propagation via TraceFilter
- @Cacheable/@CacheEvict with RedisCacheManager (agents=10m, tools=30m, permissions=1h)
- AES-256-GCM encryption via CryptoUtils for sensitive data
- Redis INCR+EXPIRE distributed rate limiting via RateLimitAspect
- DOMPurify XSS protection for v-html rendering
- @Async ThreadPoolTaskExecutor (core=5, max=20, queue=100)
- BCryptPasswordEncoder(12) financial-grade password hashing
- 128 @Transactional(rollbackFor = Exception.class) enforced
- API retry mechanism for GET requests (2x on network/5xx)
- ErrorBoundary Vue component with onErrorCaptured

### Round 21-50 (Deep Iteration)
- Comprehensive i18n: 900+ keys across 15+ namespaces in zh-CN/en-US
- Dark mode: Tailwind dark: prefix + CSS variable overrides for all pages
- Form validation: 3 pages with real-time validation rules
- Observability: traceId/MDC, response time, slow request logging
- Build optimization: chunk splitting, lazy loading
- Accessibility: aria-labels, keyboard navigation, focus management
- Responsive design: mobile-friendly layouts

### Round 51-70 (Security Hardening & Caching)
- CryptoUtils: AES-256-GCM encrypt/decrypt for API keys at rest
- RateLimitAspect: Redis INCR+EXPIRE with fallback
- CacheConfig: RedisCacheManager with per-cache TTL
- AsyncConfig: ThreadPoolTaskExecutor for async operations
- OpenAPI documentation: real /v3/api-docs integration
- MarkdownRenderer: DOMPurify + img loading=lazy
- Security headers: X-Content-Type-Options, X-Frame-Options, CSP
- 25 DTOs: @Schema annotations + enhanced validation
- Password policy: AuthService register/changePassword/resetPassword

### Round 71-80 (DevOps & Monitoring)
- GitHub Actions CI: frontend (type-check+build) + backend (compile+test)
- Prometheus custom metrics: agent_invocations_total, execution_duration_seconds, active_users_gauge
- Graceful shutdown: 30s timeout-per-shutdown-phase
- Docker resource limits: mem_limit/cpus for all 6 services
- Actuator security: /actuator/health + /actuator/info permitAll
- .gitignore: backend (target/, *.class) + frontend (node_modules/, dist/)
- Startup logging: active profile, port, masked DB URL, Redis, JWT config
- Frontend env: VITE_ENABLE_MOCK, VITE_LOG_LEVEL

### Round 81-90 (Testing Infrastructure)
- EmptyState component: 13 unit tests (5 state types)
- Auth API: 9 unit tests with mock axios
- AgentController: 8 integration tests (@SpringBootTest + MockMvc + H2)
- Security test: 16 tests (401/403/public endpoint coverage)
- Playwright E2E: login page load + form validation
- application-test.yml: H2 in-memory DB, Flyway disabled

### Round 91-100 (Final Audit & Cleanup)
- **Security**: CryptoUtils @PostConstruct validation (no insecure defaults), CSP configurable per environment
- **Architecture**: @Transactional(rollbackFor) on 6 services, @Valid on 5 controllers, ADMIN role check
- **API Contracts**: Fixed 3 experience.ts path mismatches, TestResult status enum alignment, added createMemory API
- **Orphan APIs**: Marked 8 unimplemented backend endpoints with comments
- **Logging**: Created logger.ts utility, replaced 44 console.log/error across 10 files
- **Type Safety**: Dashboard.vue `as any` -> ChartOptions, duplicate i18n key fix
- **i18n Completion**: 100+ new keys, StatusBadge 30 labels, 6 evolution/designer components, 8 pages fully i18n'd
- **Error Handling**: Fixed empty catch blocks, replaced e.printStackTrace() with SLF4J
- **Build**: All chunks < 520KB, clean build in 12s

## Quality Metrics (Round 100)
- **i18n Coverage**: 900+ keys in zh-CN/en-US, zero hardcoded user-facing strings in core components
- **Type Safety**: Zero `as any` in production code, zero `@ts-ignore`
- **Security**: AES-256-GCM encryption, BCrypt(12), CSP, CORS env-driven, JWT issuer validation
- **Architecture**: Zero Controller->Repository violations, 128 @Transactional, 22 @Valid
- **Testing**: 46 test cases (unit + integration + E2E + security)
- **Build**: Clean vite build 12s, all chunks < 520KB gzip < 171KB
- **DevOps**: CI/CD pipeline, Prometheus metrics, graceful shutdown, Docker resource limits

### Round 101-110 (Architecture Cleanup & Performance)

#### Round 101-103: Entity→VO + Deprecated Cleanup
- 4 VO classes created: ApiInterfaceVO, AgentApprovalVO, AlertRecordVO, SystemLogVO
- 2 DTOs for ApiInterface (Create/Update), replacing direct Entity exposure
- Deprecated LLM providers (QwenLlmProvider, OpenAiLlmProvider) removed from Spring context
- Deprecated LlmService marked @Deprecated
- 2 deprecated Result.fail() methods removed, 17 call sites migrated to Result.error()
- 25 @SuppressWarnings("unchecked") annotated with explanatory comments
- 15 Java files: wildcard imports replaced with explicit imports
- Dead code removed: GraphEdge commented-out field, unused imports in 4 files

#### Round 104-106: Message Code i18n + Type Safety
- 70+ message codes in messages.properties + messages_zh_CN.properties
- MessageSourceConfig + MessageUtils utility for backend i18n
- ResultCode enum: messageCode field added (backward compatible)
- BusinessException: new constructors with messageCode support
- Result: messageCode field + withMessageCode() chain method
- GlobalExceptionHandler: all handlers return messageCode in API response
- StreamController: Map<String,Object> -> StreamAgentExecutionRequestDTO
- AlertController: AlertRule entity -> AlertRuleCreateDTO/UpdateDTO
- 70+ TypeScript errors fixed across 15 frontend files
- Production code: zero as any, zero @ts-ignore, clean build

#### Round 107-109: Performance + DB Optimization + Security
- 32 routes with webpackChunkName for clear chunk naming
- 5 heavy components -> defineAsyncComponent
- Smart route prefetching with requestIdleCallback
- 3 modulepreload hints in index.html
- V23 Flyway migration: 20+ performance indexes across 9 tables
- OOM fix: DataRetentionPolicyService bulk DELETE instead of findAll+delete
- 4 unpaginated endpoints -> paginated (Suggestions, Experiences, Alerts, McpTools)
- DataExportService: max record limits (10k/50k)
- errorMessageMapper.ts: 30 messageCode->i18n key mappings
- request.ts: error interceptor uses messageCode for i18n
- 30 error message i18n keys in zh-CN/en-US
- CSP frame-ancestors: 'none' -> 'self', X-Frame-Options: SAMEORIGIN
- Permissions-Policy meta tag added

## Quality Metrics (Round 110)
- **i18n Coverage**: 960+ keys in zh-CN/en-US, backend messageCode mechanism, frontend errorMessageMapper
- **Type Safety**: Zero `as any` in production, zero `@ts-ignore`, 70+ TS errors fixed
- **Security**: AES-256-GCM, BCrypt(12), CSP configurable, messageCode i18n, Permissions-Policy
- **Architecture**: Zero Entity exposure in Controllers (all VO/DTO), 128 @Transactional, 27 @Valid
- **Testing**: 46 test cases (unit + integration + E2E + security)
- **Performance**: 20+ DB indexes, bulk DELETE, paginated endpoints, async components, route prefetch
- **Build**: Clean build 12.37s, named chunks, zero warnings
- **DevOps**: CI/CD, Prometheus, graceful shutdown, Docker limits, V23 migration
