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
