# Contributing to AegisNexus

Thank you for contributing to AegisNexus.

## Development Environment

### Prerequisites
- JDK 17+
- Node.js 20+
- PostgreSQL 16+
- Redis 7+

### Backend Development
```bash
cd backend
mvn compile "-Dmaven.test.skip=true"
mvn spring-boot:run
```

### Frontend Development
```bash
cd frontend
npm ci
npm run dev
```

## Coding Standards

### Backend (Java)
- Controllers use DTOs/VOs and must not expose Entities directly.
- Validate API inputs with `@Valid` and DTO validation annotations.
- Use `@Transactional` where business consistency requires it.
- Use structured logging through SLF4J; do not use `System.out`.
- Add or update tests for new behavior.
- Keep tenant isolation, permissions, audit, and security checks explicit.

### Frontend (Vue/TypeScript)
- Use Vue 3 Composition API with `<script setup>`.
- Use TypeScript types for component props, API responses, and store state.
- Prefer shared components from `@/components`.
- Route API calls through `@/api` modules and shared request utilities.
- Use Pinia for state management.
- Put user-facing text behind i18n keys.

### Repository Language
- Repository language is English.
- Comments, documentation, commit messages, PR descriptions, and Skill content must be written in English.
- Chat with the owner remains Simplified Chinese unless requested otherwise.

### Git Commit Convention
- `feat:` new capability
- `fix:` bug fix
- `docs:` documentation update
- `refactor:` behavior-preserving refactor
- `test:` test changes
- `chore:` build/tooling/maintenance changes

## Pull Request Process
1. Create a feature branch from `master`.
2. Keep changes small and reviewable.
3. Run the relevant verification gates.
4. Submit a PR with a clear summary, verification result, and remaining risks.
5. Require code review before merge.

## Verification
- Backend main sources: `mvn compile "-Dmaven.test.skip=true"`
- Backend test sources: `mvn test-compile`
- Backend tests: `mvn test`
- Frontend type-check: `npm run type-check`
- Frontend tests: `npm run test:run`
- Frontend build: `npm run build`

## AI Collaboration

- All AI agents must read `AGENTS.md` first.
- v2 Core work follows `docs/PLATFORM_V2_EXECUTION_PLAN.md`.
- Prefer project Skills under `.cursor/skills/`.
- Do not infer current completion status from old Round-based documents; status must be verified through code or commands.
- Use `platform-v2-code-review` before declaring complex work complete.
- Use `platform-v2-commit-discipline` before creating commits.

## License
MIT
