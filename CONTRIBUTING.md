# Contributing to AI Agent Station

感谢您对 AI Agent Station 的贡献！

## 开发环境

### 前置要求
- JDK 17+
- Node.js 20+
- PostgreSQL 16+
- Redis 7+

### 后端开发
```bash
cd backend
mvn clean package -DskipTests
java -jar target/ai-agent-platform-1.0.0.jar
```

### 前端开发
```bash
cd frontend
npm install
npm run dev
```

## 代码规范

### 后端 (Java)
- 遵循阿里巴巴 Java 开发手册
- Controller 层使用 DTO/VO，禁止直接返回 Entity
- 所有 API 入参使用 `@Valid` 校验
- Service 方法添加 `@Transactional` 注解
- 使用 `@Slf4j` 进行日志记录，禁止 `System.out`
- 新增方法必须编写单元测试

### 前端 (Vue/TypeScript)
- 使用 Vue 3 Composition API + `<script setup>`
- 组件使用 TypeScript 强类型
- 优先使用 `@/components` 中的通用组件
- API 调用统一通过 `@/api` 模块
- 状态管理使用 Pinia Store
- 文本使用 i18n 翻译 key

### Git 提交规范
- `feat:` 新功能
- `fix:` 修复 Bug
- `docs:` 文档更新
- `refactor:` 代码重构
- `test:` 测试相关
- `chore:` 构建/工具变更

## Pull Request 流程
1. 从 master 创建 feature 分支
2. 提交 PR 并填写变更说明
3. CI 自动运行测试（后端 Maven Test + 前端 Vitest）
4. 至少 1 人 Code Review
5. 合并后自动触发 CI 构建

## 测试
- 后端: `mvn test` (JUnit 5 + Mockito)
- 前端: `npx vitest run` (Vitest + Vue Test Utils)
- 覆盖率要求: 核心业务逻辑 > 60%

## License
MIT
