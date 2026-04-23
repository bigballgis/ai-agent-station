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
- **Frontend**: Vue 3 + TypeScript + Vite + Ant Design Vue + Tailwind CSS
- **Backend**: Spring Boot 3 + Java 17 + Spring Security + JWT
- **Database**: MySQL + Redis (cluster)
- **Build**: Docker Compose (multi-container)
- **CI/CD**: GitHub Actions (planned)

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
