# AI Agent Station — 全面深度优化规格文档

> **版本**: v1.0
> **日期**: 2026-04-21
> **范围**: 代码质量与架构 · 性能优化 · 安全性加固 · UI/UX 极致美化（OA布局 + 苹果交互理念）

---

## 一、项目现状概述

AI Agent Station 是一个企业级 AI Agent 管理平台，采用前后端分离架构：
- **后端**: Spring Boot 3.2.4 + Java 17 + PostgreSQL + Redis + LangChain4j
- **前端**: Vue 3 + TypeScript + Ant Design Vue 4 + Tailwind CSS + Pinia + Vite

### 核心问题诊断（共发现 80+ 项问题）

| 维度 | 严重 | 高 | 中 | 低 | 合计 |
|------|------|-----|-----|-----|------|
| 安全性 | 9 | 4 | 3 | 1 | **17** |
| 代码架构 | 3 | 5 | 6 | 3 | **17** |
| 性能 | 2 | 4 | 4 | 1 | **11** |
| 前端 UI/UX | 2 | 6 | 8 | 4 | **20** |
| 数据库 | 2 | 2 | 3 | 1 | **8** |
| 部署/基础设施 | 3 | 5 | 4 | 3 | **15** |

---

## 二、优化目标

### 2.1 总体目标
将 AI Agent Station 从"功能可用"提升到"生产级企业平台"标准，实现：
1. **安全可信** — 零硬编码密钥、完善的 RBAC 权限、输入校验全覆盖
2. **架构优雅** — 分层清晰、DTO 隔离、代码零重复、设计模式合理运用
3. **性能卓越** — 首屏加载 < 2s、API 平均响应 < 200ms、缓存命中率 > 80%
4. **极致体验** — 苹果级交互设计、OA 办公布局、流畅动画、暗色模式、响应式适配

### 2.2 设计理念：苹果交互 + OA 布局

**苹果交互理念**：
- **简洁克制** — 去除一切不必要的装饰，让内容成为主角
- **流畅动效** — 每个交互都有 200-400ms 的丝滑过渡动画
- **毛玻璃质感** — 大量使用 backdrop-blur、半透明层叠
- **圆角一致性** — 统一 12px/16px 圆角体系
- **微交互反馈** — 按钮 hover/press 状态、列表项 hover 高亮、状态切换动画
- **留白呼吸** — 充分利用空间留白，避免信息拥挤

**OA 办公布局**：
- **顶部导航栏** — 品牌 Logo + 全局搜索 + 通知中心 + 用户头像下拉
- **左侧折叠菜单** — 支持展开/收起，图标+文字双模式，分组折叠
- **主内容区** — 面包屑导航 + 页面标题 + 操作栏 + 内容区
- **右侧面板**（可选） — 属性详情、快捷操作、上下文信息
- **底部状态栏** — 系统状态、在线用户数、版本信息

---

## 三、详细优化规格

### 模块 A：安全性加固（17 项）

#### A1. 移除所有硬编码敏感信息 [已完成]
- **现状**: ~~JWT Secret、数据库密码、Redis 密码全部硬编码在 application.yml 中~~
- **规格**: 所有敏感配置通过环境变量 `${VAR_NAME}` 注入，移除默认值
- **涉及文件**: `application.yml`, `docker-compose.yml`, `k8s/*.yml`
- **验收**: `grep -r "password.*postgres\|secret.*2024" backend/` 返回空
- **实际状态**: 已完成。`application.yml` 中 `jwt.secret: ${JWT_SECRET}`（无默认值），`password: ${DB_PASSWORD:}`，`password: ${REDIS_PASSWORD:}`。所有 LLM API Key 均通过环境变量注入。`.env.example` 已创建。

#### A2. 完善 RBAC 权限控制 [P0-严重]
- **现状**: `@RequiresPermission`/`@RequiresRole` 注解已定义但未在任何 Controller 使用，`PermissionAspect.checkPermission()` 只记日志不校验
- **规格**:
  1. 修复 `PermissionAspect` 实现真实的权限校验逻辑
  2. 为所有 Controller 方法添加权限注解（超级管理员、租户管理员、普通用户三级）
  3. 完善 `SecurityConfig` 的接口级权限控制
- **涉及文件**: `SecurityConfig.java`, `PermissionAspect.java`, 所有 Controller
- **验收**: 未授权用户访问管理接口返回 403

#### A3. 输入校验全覆盖 [P0-严重]
- **现状**: 所有 Controller 的 `@RequestBody` 参数均缺少 `@Valid` 注解
- **规格**:
  1. 为所有 DTO 添加 `@NotBlank`, `@Size`, `@Email`, `@Pattern` 等校验注解
  2. 所有 Controller 方法参数添加 `@Valid`
  3. 全局异常处理器捕获 `MethodArgumentNotValidException` 并返回友好错误信息
- **涉及文件**: 所有 Controller, 所有 DTO, `GlobalExceptionHandler.java`
- **验收**: 发送空 username/password 的登录请求返回 400 和具体校验错误

#### A4. CORS 配置收紧 [P0-严重]
- **现状**: `addAllowedOriginPattern("*")` 允许所有来源
- **规格**: 通过配置文件管理允许的域名列表，生产环境仅允许指定域名
- **涉及文件**: `CorsConfig.java`, `application.yml`

#### A5. 移除 Mock 登录回退 [P0-严重]
- **现状**: `Login.vue` 登录失败后使用 Mock Token 绕过认证
- **规格**: 移除所有 Mock 登录逻辑，登录失败显示真实错误信息
- **涉及文件**: `Login.vue`

#### A6. 修复 API Key 传递方式 [P1-高]
- **现状**: API Key 可通过 URL 查询参数传递
- **规格**: 仅允许通过 `Authorization: Bearer <api_key>` 或 `X-API-Key` 请求头传递
- **涉及文件**: `JwtAuthenticationFilter.java`

#### A7. 修复 TenantContextHolder 泄漏 [P1-高]
- **现状**: ThreadLocal 未在请求结束时清理
- **规格**: 在 `TenantInterceptor.afterCompletion()` 中调用 `TenantContextHolder.clear()`
- **涉及文件**: `TenantInterceptor.java`, `WebMvcConfig.java`

#### A8. 修复 X-Tenant-Id/X-User-Id 可伪造 [P1-高]
- **现状**: 客户端可通过请求头伪造租户和用户 ID
- **规格**: 从 JWT Token 中提取用户 ID 和租户 ID，忽略客户端传入的头信息
- **涉及文件**: `AgentApiController.java`

#### A9. AgentApiController 密码字段脱敏 [P1-高]
- **现状**: User Entity 的 password 字段通过 JSON 序列化暴露
- **规格**: 使用 `@JsonIgnore` 注解或 DTO 隔离
- **涉及文件**: `User.java`, 所有涉及 User 的 Controller

#### A10. 密码重置增加旧密码验证 [P1-中]
- **涉及文件**: `UserController.java`

#### A11. Nginx 添加安全头 [P1-高]
- **规格**: 添加 `X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`, `Strict-Transport-Security`
- **涉及文件**: `docker/nginx/nginx.conf`

#### A12. Redis 集群添加密码认证 [P1-高]
- **涉及文件**: `docker/redis/cluster/redis-{1..6}.conf`, `application.yml`

#### A13. Actuator 端点权限控制 [P1-中]
- **规格**: `show-details: when-authorized`，`/actuator` 端点需 ADMIN 角色
- **涉及文件**: `application.yml`, `SecurityConfig.java`

#### A14. API Key 密码复杂度校验 [P1-中]
- **涉及文件**: `TenantController.java`

#### A15. 请求体大小限制 [P1-中]
- **规格**: Nginx `client_max_body_size 50m`，Spring Boot `max-file-size: 50MB`
- **涉及文件**: `nginx.conf`, `application.yml`

#### A16. 日志脱敏 [P1-低]
- **规格**: RequestLoggingFilter 对密码、API Key 等敏感字段脱敏
- **涉及文件**: `RequestLoggingFilter.java`

#### A17. Kubernetes Secret 管理 [P0-严重]
- **规格**: 所有密码通过 K8s Secret 注入，移除明文密码
- **涉及文件**: `k8s/*.yml`

---

### 模块 B：代码架构优化（17 项）

#### B1. 引入 DTO 层，隔离 Entity 与 API [已完成]
- **现状**: ~~所有 Controller 直接接收/返回 Entity，敏感字段暴露~~
- **规格**:
  1. 为每个 Entity 创建 RequestDTO 和 ResponseDTO
  2. 使用 MapStruct（已引入但未使用）进行 Entity ↔ DTO 转换
  3. ResponseDTO 中排除 password、apiKey、apiSecret 等敏感字段
- **涉及文件**: 新建 `dto/` 包，所有 Controller, 所有 Service
- **验收**: API 响应中不包含 password、apiKey 等敏感字段
- **实际状态**: 已完成。所有 27 个 Controller 均已使用 DTO 接收参数和返回结果。`dto/` 包包含 30+ DTO 类（AgentDTO、UserDTO、LoginRequest、TenantDTO 等）。`DTOConverter` 提供转换方法。敏感字段已通过 ResponseDTO 隔离。

#### B2. 消除 Controller 中的 Map<String, Object> 参数 [P0-严重]
- **现状**: `AgentApprovalController`、`DeploymentController` 使用 Map 接收参数
- **规格**: 创建对应的 RequestDTO，使用类型安全的参数接收
- **涉及文件**: `AgentApprovalController.java`, `DeploymentController.java`

#### B3. 统一 API 路径版本管理 [已完成]
- **现状**: ~~三种路径风格并存（无前缀、/api/、/api/v1/）~~
- **规格**: 统一为 `/api/v1/` 前缀，所有 Controller 添加 `@RequestMapping("/api/v1/xxx")`
- **涉及文件**: 所有 Controller, 前端 `request.ts` baseURL
- **实际状态**: 已完成。后端 `server.servlet.context-path=/api`，所有 27 个 Controller 使用 `@RequestMapping("/v1/*")`，完整路径为 `/api/v1/*`。前端 `request.ts` baseURL 为 `http://localhost:8080/api`，API 调用使用 `/v1/*` 相对路径。

#### B4. 统一 RESTful API 规范 [P1-中]
- **现状**: `DELETE /roles/remove?userId=x&roleId=y` 等不规范路径
- **规格**:
  - `POST /roles/assign` → `PUT /api/v1/roles/{roleId}/users/{userId}`
  - `DELETE /roles/remove` → `DELETE /api/v1/roles/{roleId}/users/{userId}`
  - `POST /permissions/assign` → `PUT /api/v1/permissions/roles/{roleId}/permissions/{permissionId}`
- **涉及文件**: `RoleController.java`, `PermissionController.java`, 前端 API 层

#### B5. 统一 API 响应格式 [P1-中]
- **现状**: AgentApiController 使用 `ResponseEntity` 直接返回，不经过 `Result` 包装
- **规格**: 所有接口统一使用 `Result<T>` 包装
- **涉及文件**: `AgentApiController.java`

#### B6. 提取 SecurityUtils 工具类 [P1-中]
- **现状**: `getCurrentUserId()` 在 3+ 个 Service 中重复
- **规格**: 抽取到 `com.aiagent.util.SecurityUtils.getCurrentUserId()`
- **涉及文件**: 新建 `SecurityUtils.java`, `AgentService.java`, `ExperienceServiceImpl.java`, `SuggestionServiceImpl.java`

#### B7. 租户隔离逻辑抽象 [P1-中]
- **现状**: 6+ 个 Service 中重复 `if (tenantId != null) { findByXxxAndTenantId } else { findAll() }` 模式
- **规格**: 创建 `TenantAwareRepository` 基类或 AOP 切面自动注入租户条件
- **涉及文件**: 新建 `TenantAwareRepository.java` 或 `TenantFilterAspect.java`

#### B8. 消除魔法数字 [P1-中]
- **现状**: `experience.setStatus(1)`, `Long userId = 1L`, `return 3` 等
- **规格**: 使用枚举类或常量类替代所有魔法数字
- **涉及文件**: `ExperienceServiceImpl.java`, `AgentExecutionEngine.java`, `SuggestionServiceImpl.java`

#### B9. 统一 Lombok 使用 [P1-中]
- **现状**: 部分类用 `@RequiredArgsConstructor`，部分手动写构造函数，Entity 全手写 getter/setter
- **规格**:
  - Entity 使用 `@Data` + `@NoArgsConstructor` + `@Builder`
  - Service 使用 `@RequiredArgsConstructor`
  - DTO 使用 `@Data` + `@Builder`
- **涉及文件**: 所有 Entity, 所有 Service, 所有 DTO

#### B10. 修复 Result.error() 与 Result.fail() 重复 [P2-低]
- **规格**: 移除 `fail()` 方法，统一使用 `error()`
- **涉及文件**: `Result.java`

#### B11. 修复 UserPrincipal 方法重复 [P2-低]
- **规格**: 移除 `getId()` 方法，统一使用 `getUserId()`
- **涉及文件**: `UserPrincipal.java`, `OperationLogAspect.java`

#### B12. 修复 Spring Cloud Gateway 与 Web MVC 冲突 [P1-中]
- **现状**: 同时引入 `spring-boot-starter-web` 和 `spring-cloud-starter-gateway`
- **规格**: 移除 `spring-cloud-starter-gateway` 依赖，网关功能通过 Nginx 实现
- **涉及文件**: `pom.xml`, 移除 `gateway/` 包

#### B13. 修复 hibernate-types 版本不兼容 [P1-中]
- **现状**: `hibernate-types-55` 与 Hibernate 6.x 不兼容
- **规格**: 替换为 `hypersistence-utils-hibernate-63`
- **涉及文件**: `pom.xml`

#### B14. 前端 TypeScript 严格模式 [P0-严重]
- **现状**: `strict: false`，大量 `any` 类型
- **规格**:
  1. 启用 `strict: true`
  2. 启用 `noUnusedLocals: true`, `noUnusedParameters: true`
  3. 为所有 API 函数定义请求/响应类型接口
  4. 逐步消除 `any` 类型
- **涉及文件**: `tsconfig.json`, 所有 `.ts`/`.vue` 文件

#### B15. 前端提取公共工具函数 [P1-高]
- **现状**: `formatDate`、`getStatusColor`、`.btn` 样式在多个文件重复
- **规格**:
  1. 创建 `src/utils/format.ts` — formatDate, getStatusColor, getStatusText
  2. 创建 `src/styles/common.css` — 公共按钮样式、页面头部样式
  3. 创建 `src/types/api.ts` — 统一的 ApiResponse<T>、PageResult<T> 类型定义
- **涉及文件**: 新建工具文件，所有页面组件

#### B16. 前端统一 API 响应处理 [P1-高]
- **现状**: `agent.ts` 返回 `res.data`，`approval.ts` 返回 `res.data?.data?.content`
- **规格**: 在 Axios 响应拦截器中标准化，所有 API 函数返回统一的 `ApiResponse<T>` 类型
- **涉及文件**: `request.ts`, 所有 `api/*.ts` 文件

#### B17. 前端移除死代码 [P2-低]
- **现状**: `Empty.vue`、`HomePage.vue`、`store/index.ts` 未被使用
- **规格**: 删除未引用的文件和依赖（flowise-components, lucide-vue-next）
- **涉及文件**: 删除文件, `package.json`

---

### 模块 C：性能优化（11 项）

#### C1. 前端路由懒加载 [P0-严重]
- **现状**: 所有 20+ 页面静态导入，打包为单一 JS chunk
- **规格**: 所有页面改为 `() => import('@/pages/Xxx.vue')` 动态导入
- **涉及文件**: `router/index.ts`
- **验收**: `dist/assets/` 中出现多个 chunk 文件

#### C2. Vite 构建分包策略 [P1-高]
- **现状**: 所有代码打包为单一 `index-*.js`
- **规格**:
  ```js
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'antd': ['ant-design-vue', '@ant-design/icons-vue'],
          'chart': ['chart.js', 'vue-chartjs'],
          'i18n': ['vue-i18n'],
        }
      }
    }
  }
  ```
- **涉及文件**: `vite.config.ts`

#### C3. Ant Design Vue 按需导入 [P1-高]
- **现状**: `import Antd from 'ant-design-vue'` 全量导入
- **规格**: 使用 `unplugin-vue-components` + `unplugin-auto-import` 自动按需导入
- **涉及文件**: `vite.config.ts`, `package.json`, `main.ts`

#### C4. 后端列表接口添加分页 [P1-高]
- **现状**: 6+ 个列表接口返回全量数据
- **规格**: 所有列表接口支持 `page`、`size`、`sort` 参数，返回 `PageResult<T>`
- **涉及文件**: 所有 Controller, 所有 Service, `PageResult.java`

#### C5. 后端 Redis 缓存策略 [P1-高]
- **现状**: Redis 已配置但几乎未使用
- **规格**:
  1. Agent 配置查询添加 `@Cacheable`（TTL 5min）
  2. 权限数据添加 `@Cacheable`（TTL 10min）
  3. 用户信息添加 `@Cacheable`（TTL 30min）
  4. 数据变更时使用 `@CacheEvict` 清除
- **涉及文件**: `AgentService.java`, `PermissionService.java`, `UserService.java`

#### C6. 异步任务持久化 [P0-严重]
- **现状**: `ConcurrentHashMap` 存储在内存中
- **规格**: 使用 Redis 或数据库存储异步任务状态
- **涉及文件**: `AgentExecutionEngine.java`

#### C7. Nginx 启用 gzip/brotli 压缩 [P1-高]
- **规格**: 添加 `gzip on` 及对应 MIME 类型配置
- **涉及文件**: `docker/nginx/nginx.conf`

#### C8. 构建产物预压缩 [P1-中]
- **规格**: Vite 构建后生成 `.gz` 和 `.br` 文件
- **涉及文件**: `vite.config.ts`（添加 `vite-plugin-compression`）

#### C9. 数据库复合索引优化 [P1-中]
- **规格**: 新增 V8 migration 添加关键复合索引
- **涉及文件**: 新建 `V8__add_performance_indexes.sql`

#### C10. 前端图片/资源优化 [P2-低]
- **规格**: favicon 使用 SVG（已满足），添加 preconnect

#### C11. 后端连接池优化 [P2-低]
- **规格**: 添加 `leak-detection-threshold: 60000`，调整 `maximum-pool-size`
- **涉及文件**: `application.yml`

---

### 模块 D：UI/UX 极致美化（20 项）

#### D1. 全新 OA 办公布局设计 [P0-核心]
- **现状**: 基础侧边栏布局，样式简陋，三套样式方案混用
- **规格**: 完全重写 `MainLayout.vue`，实现苹果级 OA 布局：
  - **顶部导航栏** (56px高):
    - 左侧: Logo + 应用名称
    - 中间: 全局搜索框（Command+K 快捷键唤起）
    - 右侧: 通知铃铛(带红点) + 暗色模式切换 + 语言切换 + 用户头像下拉菜单
    - 样式: `backdrop-blur-xl bg-white/80 dark:bg-gray-900/80 border-b border-gray-200/50`
  - **左侧菜单** (240px/64px):
    - 支持展开/收起动画（300ms cubic-bezier）
    - 菜单分组（Agent管理、测试中心、系统设置等）
    - 图标 + 文字模式，收起时仅显示图标
    - 当前选中项: 左侧 3px 蓝色指示条 + 浅蓝背景
    - Hover: 微妙背景变化
    - 样式: `bg-gray-50 dark:bg-gray-900` + 毛玻璃效果
  - **主内容区**:
    - 面包屑导航（带图标）
    - 页面标题 + 描述 + 操作按钮
    - 内容区带 `p-6` 内边距
    - 底部留白
  - **底部状态栏** (32px高):
    - 左侧: 系统状态指示灯（绿色=正常）
    - 右侧: 在线用户数 + 版本号
- **涉及文件**: `MainLayout.vue`（完全重写）, `App.vue`, `style.css`

#### D2. 统一设计系统 (Design Tokens) [P0-核心]
- **规格**: 在 Tailwind 配置中定义完整的设计 Token：
  ```js
  theme: {
    extend: {
      colors: {
        primary: { 50-950: 蓝色系 },
        neutral: { 50-950: 灰色系 },
        success: { 50-950: 绿色系 },
        warning: { 50-950: 橙色系 },
        danger: { 50-950: 红色系 },
      },
      borderRadius: {
        'xl': '12px',
        '2xl': '16px',
        '3xl': '20px',
      },
      boxShadow: {
        'glass': '0 8px 32px rgba(0,0,0,0.06)',
        'card': '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)',
        'float': '0 20px 40px rgba(0,0,0,0.08)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s cubic-bezier(0.16,1,0.3,1)',
        'slide-in-left': 'slideInLeft 0.3s cubic-bezier(0.16,1,0.3,1)',
        'scale-in': 'scaleIn 0.2s cubic-bezier(0.16,1,0.3,1)',
      }
    }
  }
  ```
- **涉及文件**: `tailwind.config.js`

#### D3. 暗色模式完整接入 [P1-高]
- **现状**: `useTheme.ts` 已实现但从未被调用
- **规格**:
  1. 在 `App.vue` 中调用 `useTheme()` 初始化暗色模式
  2. 所有页面组件使用 `dark:` 前缀适配暗色
  3. Ant Design Vue 通过 `a-config-provider` 的 `theme` 属性切换算法
  4. 侧边栏和顶栏适配暗色
- **涉及文件**: `App.vue`, `MainLayout.vue`, `useTheme.ts`, 所有页面组件

#### D4. 路由过渡动画 [P1-高]
- **现状**: 无任何路由切换动画
- **规格**:
  ```vue
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  ```
  - 页面进入: `fadeIn + slideUp` (0.3s)
  - 页面离开: `fadeOut` (0.15s)
- **涉及文件**: `App.vue`, `style.css`

#### D5. 列表页面统一重构 [P1-高]
- **现状**: 每个列表页面样式不一致，部分使用 Ant Design Table，部分自定义卡片
- **规格**: 创建统一的列表页面模板：
  - 页面头部: 标题 + 描述 + 新建按钮 + 搜索框 + 筛选器
  - 数据表格: Ant Design Table，统一列样式、操作列
  - 分页: 统一分页组件
  - 空状态: 统一的空状态插图 + 引导文案
  - 加载状态: 骨架屏
- **涉及文件**: 所有列表页面（AgentList, TestCaseList, ApprovalManagement 等）

#### D6. 卡片组件统一设计 [P1-高]
- **现状**: AgentList 的卡片样式简陋
- **规格**: 苹果风格卡片设计：
  - 圆角 16px，微妙阴影
  - Hover: 轻微上浮 + 阴影加深 (transition 0.2s)
  - 内容区: 图标/头像 + 标题 + 描述 + 标签 + 底部操作
  - 毛玻璃效果: `backdrop-blur-sm bg-white/70`
- **涉及文件**: `AgentList.vue`, 新建 `AgentCard.vue` 组件

#### D7. 表单页面统一设计 [P1-中]
- **规格**:
  - 表单容器: 居中卡片，最大宽度 720px
  - 输入框: 统一圆角 8px，聚焦时蓝色边框 + 微妙阴影
  - 按钮: 主按钮蓝色渐变，次按钮灰色边框
  - 表单验证: 实时校验 + 红色提示文字
- **涉及文件**: `AgentEdit.vue`, `TestCaseEdit.vue`, `Login.vue`

#### D8. Dashboard 数据可视化美化 [P1-高]
- **现状**: 基础统计卡片，样式普通
- **规格**:
  - 统计卡片: 渐变背景 + 大数字 + 趋势箭头 + 迷你图表
  - 图表区: 使用 Chart.js 绘制折线图、饼图、柱状图
  - 快捷操作: 圆角图标按钮网格
  - 最近活动: 时间线样式
- **涉及文件**: `Dashboard.vue`

#### D9. 登录页面重新设计 [P1-高]
- **现状**: 基础登录表单
- **规格**:
  - 左右分栏布局（大屏） / 居中卡片（小屏）
  - 左侧: 品牌插图/动画 + 产品介绍
  - 右侧: 登录表单（毛玻璃卡片）
  - 背景: 渐变色 + 动态粒子/几何图形
  - 输入框: 浮动标签动画
  - 登录按钮: 渐变 + hover 发光效果
- **涉及文件**: `Login.vue`（完全重写）

#### D10. 全局搜索组件 [P2-中]
- **规格**: Command+K 唤起全局搜索弹窗（类似 Spotlight/Raycast）
  - 搜索 Agent、测试用例、审批单等
  - 键盘导航（上下箭头 + Enter）
  - 最近搜索记录
- **涉及文件**: 新建 `GlobalSearch.vue`

#### D11. 通知中心 [P2-中]
- **规格**: 顶栏铃铛图标 + 下拉通知面板
  - 未读数量红点
  - 通知列表（审批通知、系统通知）
  - 标记已读/全部已读
- **涉及文件**: 新建 `NotificationCenter.vue`

#### D12. 骨架屏加载状态 [P1-中]
- **规格**: 为所有数据加载场景提供骨架屏
  - Dashboard 统计卡片骨架
  - 列表页面表格骨架
  - 详情页面内容骨架
- **涉及文件**: 新建 `SkeletonCard.vue`, `SkeletonTable.vue`, 各页面

#### D13. 空状态统一组件 [P1-中]
- **现状**: 大部分列表页面缺少空状态
- **规格**: 创建 `EmptyState.vue` 组件
  - SVG 插图 + 标题 + 描述 + 操作按钮
  - 支持多种预设类型（无数据、搜索无结果、网络错误、权限不足）
- **涉及文件**: 新建 `EmptyState.vue`, 所有列表页面

#### D14. 响应式适配 [P1-中]
- **现状**: 侧边栏固定宽度，无移动端适配
- **规格**:
  - < 768px: 侧边栏变为抽屉式（overlay），顶栏简化
  - 768-1024px: 侧边栏默认收起
  - > 1024px: 侧边栏默认展开
  - 所有表格支持横向滚动
  - 卡片网格自适应列数
- **涉及文件**: `MainLayout.vue`, 所有页面组件

#### D15. 国际化全覆盖 [P1-高]
- **现状**: 大量硬编码中文字符串，部分缺失 i18n key
- **规格**:
  1. 所有硬编码字符串迁移到 `zh-CN.ts` 和 `en-US.ts`
  2. 补全缺失的 i18n key（common.versions, common.copy 等）
  3. Dashboard、NotFound、Placeholder 页面接入 i18n
- **涉及文件**: `zh-CN.ts`, `en-US.ts`, 所有页面组件

#### D16. 统一样式方案 [P1-中]
- **现状**: Tailwind CSS + Scoped CSS + Ant Design 样式三套混用
- **规格**:
  - 主方案: Tailwind CSS（布局、间距、颜色、响应式）
  - 组件样式: Ant Design Vue（通过 Config Provider 自定义主题 Token）
  - 自定义样式: 最小化 Scoped CSS，仅用于复杂动画和特殊效果
  - 配置 `corePlugins: { preflight: false }` 避免与 Ant Design 冲突
- **涉及文件**: `tailwind.config.js`, 所有 `.vue` 文件

#### D17. 拆分 AgentCanvas 组件 [P2-中]
- **现状**: 387 行单文件，集成所有功能
- **规格**: 拆分为 `NodePanel`, `CanvasArea`, `PropertiesPanel`, `ConnectionManager` 子组件
- **涉及文件**: `AgentCanvas.vue`

#### D18. 进化模块接入真实 API [P2-中]
- **现状**: ExperienceData、OptimizationSuggestion、ReflectionEvaluation 使用硬编码 Mock 数据
- **规格**: 接入后端 API，替换 Mock 数据
- **涉及文件**: `evolution/*.vue`, `api/` 层

#### D19. API 管理页面接入真实 API [P2-中]
- **现状**: ApiManagement、ApiDocumentation 使用 Mock 数据
- **规格**: 接入后端 API
- **涉及文件**: `ApiManagement.vue`, `ApiDocumentation.vue`

#### D20. 修复前端已知 Bug [P1-高]
- **规格**:
  1. 修复 `ApprovalManagement.vue` 的 JSX 语法 `width={800}` → `:width="800"`
  2. 修复 `TestExecutionList.vue` 的定时器内存泄漏
  3. 修复 API URL 路径重复（`/api/api/approvals`）
  4. 统一 API 模块导入路径（全部使用 `@/utils/request`）
- **涉及文件**: 对应页面文件

---

### 模块 E：数据库优化（8 项）

#### E1. 修复 SQL Migration 重复代码 [P0-严重]
- **现状**: `create_tenant_schema()` 在 V1、V6、V7 中重复定义（每次约 300 行）
- **规格**:
  1. V1 保留完整的 `create_tenant_schema()` 函数
  2. V6、V7 改为 `ALTER TABLE` 或 `CREATE TABLE IF NOT EXISTS` 增量语句
  3. 不再重复定义整个函数
- **涉及文件**: `V6__add_test_tables.sql`, `V7__add_evolution_tables.sql`

#### E2. 合并两套租户表结构 [P0-严重]
- **现状**: V1 的 `tenant` 表和 V2 的 `tenants` 表并存，结构不一致
- **规格**: 统一使用 `tenants` 表，创建 V8 migration 迁移数据并删除旧表
- **涉及文件**: 新建 `V8__merge_tenant_tables.sql`

#### E3. 修复 V6 外键引用错误 [P1-高]
- **现状**: `REFERENCES agent(id)` 应为 `REFERENCES agents(id)`
- **规格**: 修复所有外键引用的表名
- **涉及文件**: `V6__add_test_tables.sql`

#### E4. users 表 username 组合唯一约束 [P1-中]
- **规格**: `UNIQUE (username)` → `UNIQUE (username, tenant_id)`
- **涉及文件**: 新建 migration

#### E5. 添加复合索引 [P1-中]
- **规格**: V8 migration 添加：
  - `idx_agent_versions_agent_version` ON `(agent_id, version_number)`
  - `idx_agent_approvals_agent_status` ON `(agent_id, status)`
  - `idx_system_logs_module_created` ON `(module, created_at)`
  - `idx_api_call_logs_agent_created` ON `(agent_id, created_at)`
- **涉及文件**: 新建 `V8__add_performance_indexes.sql`

#### E6. PageResult 补充分页元数据 [P2-低]
- **规格**: 添加 `page`, `size`, `totalPages` 字段
- **涉及文件**: `PageResult.java`

#### E7. 清理 JDBC URL 中的 MySQL 参数 [P2-低]
- **现状**: PostgreSQL URL 包含 `useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai`
- **涉及文件**: `application.yml`

#### E8. 数据库连接池监控 [P2-低]
- **规格**: 添加 `register-mbeans: true`，配置 Prometheus 指标采集
- **涉及文件**: `application.yml`

---

### 模块 F：部署与基础设施（15 项）

#### F1. 创建后端 Dockerfile [P0-严重]
- **现状**: `docker-compose.yml` 引用了 `./backend/Dockerfile` 但文件不存在
- **规格**: 多阶段构建（Maven 构建 + JRE 运行）
- **涉及文件**: 新建 `backend/Dockerfile`

#### F2. 创建前端 Dockerfile [P0-严重]
- **现状**: `./Dockerfile.frontend` 不存在
- **规格**: 多阶段构建（Node 构建 + Nginx 运行）
- **涉及文件**: 新建 `Dockerfile.frontend`

#### F3. Docker Compose 敏感信息管理 [P0-严重]
- **现状**: 数据库密码、Grafana 密码硬编码
- **规格**: 创建 `.env.example` 模板，使用 `${VAR}` 引用
- **涉及文件**: `.env.example`, `docker-compose.yml`

#### F4. K8s Ingress 完善 [P1-高]
- **现状**: 仅代理前端，缺少 `/api` 路由和 TLS
- **规格**: 添加 `/api` 路由到 backend Service，配置 TLS
- **涉及文件**: `k8s/frontend.yml`

#### F5. K8s 后端添加 startupProbe [P1-高]
- **规格**: 添加 startupProbe 避免应用启动慢导致 livenessProbe 误杀
- **涉及文件**: `k8s/backend.yml`

#### F6. K8s 添加 PodDisruptionBudget [P1-中]
- **涉及文件**: 新建 `k8s/pdb.yml`

#### F7. K8s Pod 反亲和性 [P1-中]
- **规格**: 添加 `podAntiAffinity` 确保副本分布在不同节点
- **涉及文件**: `k8s/backend.yml`

#### F8. Prometheus 告警规则 [P1-高]
- **规格**: 创建告警规则文件（API 错误率 > 5%、响应时间 > 1s、CPU > 80%）
- **涉及文件**: 新建 `docker/prometheus/alert_rules.yml`

#### F9. Grafana Dashboard 配置 [P1-高]
- **规格**: 创建预置监控面板（API 性能、JVM、数据库连接池）
- **涉及文件**: 新建 `docker/grafana/dashboards/*.json`

#### F10. 添加 Redis/PostgreSQL Exporter [P1-中]
- **涉及文件**: `docker-compose.yml`

#### F11. Docker 日志限制 [P1-中]
- **规格**: 配置 `logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }`
- **涉及文件**: `docker-compose.yml`

#### F12. 前端 HPA 配置 [P2-低]
- **涉及文件**: `k8s/frontend.yml`

#### F13. mise.toml 添加 Node.js [P2-低]
- **涉及文件**: `mise.toml`

#### F14. index.html 修正 [P2-低]
- **规格**: `lang="zh-CN"`, title 改为 "AI Agent Platform"
- **涉及文件**: `index.html`

#### F15. CI/CD 流水线 [P2-低]
- **规格**: GitHub Actions（lint + test + build + deploy）
- **涉及文件**: 新建 `.github/workflows/*.yml`

---

## 四、优化优先级矩阵

### Phase 1 — 安全与稳定性（P0，必须立即修复）
| 序号 | 任务 | 模块 | 预估工时 |
|------|------|------|----------|
| 1 | 移除所有硬编码敏感信息 | A1, F3 | 2h |
| 2 | 完善 RBAC 权限控制 | A2 | 4h |
| 3 | 输入校验全覆盖 | A3 | 3h |
| 4 | CORS 配置收紧 | A4 | 0.5h |
| 5 | 移除 Mock 登录回退 | A5 | 0.5h |
| 6 | 引入 DTO 层 | B1 | 6h |
| 7 | 消除 Map<String, Object> 参数 | B2 | 2h |
| 8 | TypeScript 严格模式 | B14 | 4h |
| 9 | 路由懒加载 | C1 | 1h |
| 10 | 异步任务持久化 | C6 | 3h |
| 11 | 修复 SQL Migration 重复 | E1 | 3h |
| 12 | 合并租户表结构 | E2 | 2h |
| 13 | 创建 Dockerfile | F1, F2 | 2h |
| 14 | K8s Secret 管理 | A17 | 1h |
| **合计** | | | **34.5h** |

### Phase 2 — 架构优化与性能提升（P1，应尽快修复）
| 序号 | 任务 | 模块 | 预估工时 |
|------|------|------|----------|
| 1 | API Key 传递方式修复 | A6 | 0.5h |
| 2 | TenantContextHolder 泄漏修复 | A7 | 0.5h |
| 3 | X-Tenant-Id/X-User-Id 伪造修复 | A8 | 1h |
| 4 | Entity 敏感字段脱敏 | A9 | 1h |
| 5 | Nginx 安全头 | A11 | 0.5h |
| 6 | Redis 集群密码 | A12 | 0.5h |
| 7 | Actuator 权限 | A13 | 0.5h |
| 8 | API 路径版本统一 | B3 | 3h |
| 9 | RESTful 规范统一 | B4 | 2h |
| 10 | 响应格式统一 | B5 | 1h |
| 11 | 提取 SecurityUtils | B6 | 1h |
| 12 | 租户隔离抽象 | B7 | 3h |
| 13 | 消除魔法数字 | B8 | 2h |
| 14 | Lombok 统一 | B9 | 3h |
| 15 | Gateway 冲突修复 | B12 | 1h |
| 16 | hibernate-types 修复 | B13 | 0.5h |
| 17 | 前端公共工具提取 | B15 | 2h |
| 18 | 前端 API 响应统一 | B16 | 2h |
| 19 | Vite 分包策略 | C2 | 1h |
| 20 | Ant Design Vue 按需导入 | C3 | 2h |
| 21 | 后端列表分页 | C4 | 3h |
| 22 | Redis 缓存策略 | C5 | 3h |
| 23 | Nginx gzip 压缩 | C7 | 0.5h |
| 24 | 构建产物预压缩 | C8 | 0.5h |
| 25 | 数据库复合索引 | E5 | 1h |
| 26 | V6 外键修复 | E3 | 0.5h |
| 27 | OA 布局重写 | D1 | 8h |
| 28 | 设计系统 Tokens | D2 | 2h |
| 29 | 暗色模式接入 | D3 | 4h |
| 30 | 路由过渡动画 | D4 | 1h |
| 31 | 列表页面重构 | D5 | 6h |
| 32 | 卡片组件设计 | D6 | 3h |
| 33 | Dashboard 美化 | D8 | 4h |
| 34 | 登录页重设计 | D9 | 4h |
| 35 | 国际化全覆盖 | D15 | 4h |
| 36 | 统一样式方案 | D16 | 3h |
| 37 | 前端 Bug 修复 | D20 | 2h |
| 38 | K8s Ingress 完善 | F4 | 1h |
| 39 | K8s startupProbe | F5 | 0.5h |
| 40 | Prometheus 告警 | F8 | 2h |
| 41 | Grafana Dashboard | F9 | 3h |
| 42 | Docker 日志限制 | F11 | 0.5h |
| **合计** | | | **97h** |

### Phase 3 — 完善与优化（P2，建议改进）
| 序号 | 任务 | 模块 | 预估工时 |
|------|------|------|----------|
| 1 | 全局搜索组件 | D10 | 4h |
| 2 | 通知中心 | D11 | 3h |
| 3 | 骨架屏 | D12 | 3h |
| 4 | 空状态组件 | D13 | 2h |
| 5 | 响应式适配 | D14 | 4h |
| 6 | 表单页面统一 | D7 | 3h |
| 7 | AgentCanvas 拆分 | D17 | 3h |
| 8 | 进化模块真实 API | D18 | 3h |
| 9 | API 管理真实 API | D19 | 3h |
| 10 | 密码重置验证 | A10 | 0.5h |
| 11 | API Key 复杂度 | A14 | 0.5h |
| 12 | 请求体大小限制 | A15 | 0.5h |
| 13 | 日志脱敏 | A16 | 1h |
| 14 | Result 方法去重 | B10 | 0.5h |
| 15 | UserPrincipal 去重 | B11 | 0.5h |
| 16 | 死代码清理 | B17 | 1h |
| 17 | PageResult 元数据 | E6 | 0.5h |
| 18 | JDBC URL 清理 | E7 | 0.5h |
| 19 | 连接池监控 | E8 | 0.5h |
| 20 | PDB + 反亲和性 | F6, F7 | 1h |
| 21 | Redis/PG Exporter | F10 | 1h |
| 22 | 前端 HPA | F12 | 0.5h |
| 23 | mise.toml + index.html | F13, F14 | 0.5h |
| 24 | CI/CD | F15 | 3h |
| **合计** | | | **45.5h** |

### 总预估工时: **177 小时**

---

## 五、当前架构约定（已实现）

以下为代码中已落地实施的架构约定，所有新功能开发必须遵循：

### 5.1 API 路径规范

| 层级 | 路径规则 | 说明 |
|------|----------|------|
| 后端 context-path | `/api` | `server.servlet.context-path=/api` |
| Controller 路由 | `/v1/*` | 所有 27 个 Controller 统一前缀 |
| 完整后端路径 | `/api/v1/*` | 如 `POST /api/v1/auth/login` |
| 前端 baseURL | `http://localhost:8080/api` | `request.ts` 中配置 |
| 前端 API 路径 | `/v1/*` | 相对路径，由 baseURL 拼接 |

### 5.2 认证接口

| 接口 | 方法 | 路径 | 行为 |
|------|------|------|------|
| 登录 | POST | `/api/v1/auth/login` | 接收 username/password/tenantId，返回 accessToken + refreshToken |
| 刷新 Token | POST | `/api/v1/auth/refresh` | 接收 refreshToken，返回新 accessToken |
| 登出 | POST | `/api/v1/auth/logout` | 使 refreshToken 失效，accessToken 加入黑名单 |
| 用户信息 | GET | `/api/v1/auth/userinfo` | 返回 `UserResponseDTO`（不含密码） |

### 5.3 Token 存储策略

前端通过 `src/utils/authStorage.ts` 管理 Token：

- **双存储**: 支持 `localStorage` 和 `sessionStorage`
- **写入规则**: `remember=true` 时存 localStorage，否则存 sessionStorage；写入时清除另一存储
- **读取规则**: 优先 localStorage，fallback 到 sessionStorage
- **清除规则**: `clearAuth()` 同时清除两个存储中的 token、refreshToken、userInfo
- **自动刷新**: `request.ts` 响应拦截器在 401 时自动调用 `/v1/auth/refresh`，支持请求队列

### 5.4 SSE 流式接口

SSE 通过 `src/api/stream.ts` 调用，使用 **POST 方法 + fetch + ReadableStream**：

- `streamChat(params)` — `POST /api/v1/stream/chat`
- `streamAgentExecution(agentId, params)` — `POST /api/v1/stream/agent/{agentId}`
- 后端 `StreamController` 同时支持 GET 和 POST，前端统一使用 POST
- SSE 事件格式: `event: token/done/error` + `data: JSON`

### 5.5 DTO 改造范围

所有 27 个 Controller 均已完成 DTO 化：
- Controller 层接收 RequestDTO，返回 `Result<ResponseDTO>`
- `dto/` 包包含 30+ DTO 类
- `DTOConverter` 提供 Entity ↔ DTO 转换方法
- 敏感字段（password、apiKey、apiSecret）通过 ResponseDTO 隔离

### 5.6 环境变量与密钥策略

所有敏感配置通过环境变量注入，`JWT_SECRET` 无默认值（必填）：

| 环境变量 | 配置项 | 默认值 |
|----------|--------|--------|
| `JWT_SECRET` | `jwt.secret` | 无（必填） |
| `JWT_EXPIRATION` | `jwt.expiration` | `3600000` (1h) |
| `JWT_REFRESH_EXPIRATION` | `jwt.refresh-expiration` | `604800000` (7d) |
| `DB_PASSWORD` | `spring.datasource.password` | 空 |
| `REDIS_PASSWORD` | `spring.data.redis.password` | 空 |
| `OPENAI_API_KEY` | `ai-agent.llm.openai.api-key` | 空 |
| `QWEN_API_KEY` | `ai-agent.llm.qwen.api-key` | 空 |

---

## 六、技术约束

1. **Java 17** — 不升级到 Java 21（保持与 mise.toml 一致）
2. **Spring Boot 3.2.4** — 不升级大版本
3. **Vue 3 + TypeScript** — 不引入新的前端框架
4. **Ant Design Vue 4** — 保留作为 UI 组件库，通过 Config Provider 自定义主题
5. **Tailwind CSS 3** — 作为主要样式方案
6. **PostgreSQL + Redis** — 不更换数据库
7. **向后兼容** — API 变更需要版本管理，旧版本 API 保留至少一个版本周期

---

## 七、成功标准

| 指标 | 当前 | 目标 |
|------|------|------|
| OWASP 安全评分 | F | A |
| TypeScript 严格模式 | 关闭 | 开启 |
| 首屏 JS 体积 | 单 chunk ~2MB | 多 chunk，首屏 < 500KB |
| 列表 API 分页 | 无 | 全部支持 |
| Redis 缓存使用 | 仅 API Key | Agent/权限/用户缓存 |
| 暗色模式 | 未接入 | 完整支持 |
| 国际化覆盖 | ~40% | 100% |
| 响应式适配 | 无 | 全断点覆盖 |
| Docker 构建 | 无法构建 | 成功构建并运行 |
| K8s 部署 | 密码明文 | Secret 管理 + TLS |
