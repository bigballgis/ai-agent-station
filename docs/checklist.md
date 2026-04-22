# AI Agent Station — 验收检查清单

> **关联规格**: [spec.md](./spec.md)
> **关联任务**: [tasks.md](./tasks.md)

---

## A. 安全性验收

### A1. 敏感信息管理
- [ ] `application.yml` 中无硬编码密码（数据库、Redis、JWT）
- [ ] `docker-compose.yml` 中无硬编码密码
- [ ] `k8s/*.yml` 中无明文密码，全部通过 Secret 引用
- [ ] `.env.example` 文件存在，包含所有必需环境变量及说明
- [ ] JWT Secret 无默认值，强制从环境变量读取

### A2. 认证与授权
- [ ] 未登录用户访问受保护接口返回 401
- [ ] 普通用户访问管理接口（/users, /roles, /tenants）返回 403
- [ ] 租户管理员只能管理本租户资源
- [ ] 超级管理员可访问所有接口
- [ ] API Key 仅通过请求头传递，URL 参数方式已禁用
- [ ] TenantContextHolder 在请求结束后已清理

### A3. 输入校验
- [ ] 所有 `@RequestBody` 参数都有 `@Valid` 注解
- [ ] 发送空 username/password 的登录请求返回 400 + 校验错误详情
- [ ] 发送超长字符串的请求被正确拒绝
- [ ] 全局异常处理器正确处理 `MethodArgumentNotValidException`

### A4. CORS 与安全头
- [ ] 非白名单域名的跨域请求被拒绝
- [ ] Nginx 响应头包含 `X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`
- [ ] Nginx 配置了 `client_max_body_size`

### A5. 前端安全
- [ ] Login.vue 中无 Mock 登录回退逻辑
- [ ] 登录失败显示真实错误信息

---

## B. 代码架构验收

### B1. DTO 层
- [ ] 所有 Controller 方法参数类型为 DTO 而非 Entity
- [ ] 所有 Controller 方法返回类型为 DTO 或 `Result<DTO>`
- [ ] API 响应 JSON 中不包含 password, apiKey, apiSecret 字段
- [ ] MapStruct Mapper 接口存在且被使用
- [ ] 无 `Map<String, Object>` 作为 Controller 方法参数

### B2. API 设计
- [ ] 所有 API 路径统一使用 `/api/v1/` 前缀
- [ ] RESTful 规范：无 `/assign`, `/remove` 等动词路径
- [ ] 所有接口统一使用 `Result<T>` 包装响应
- [ ] AgentApiController 不再使用 `ResponseEntity` 直接返回

### B3. 代码质量
- [ ] `getCurrentUserId()` 方法仅在 `SecurityUtils` 中定义一份
- [ ] 无魔法数字（所有状态码使用枚举或常量）
- [ ] Lombok 使用一致（Entity 用 @Data, Service 用 @RequiredArgsConstructor）
- [ ] `Result.error()` 和 `Result.fail()` 不重复
- [ ] `UserPrincipal` 无重复方法
- [ ] 无 Spring Cloud Gateway 依赖冲突
- [ ] hibernate-types 版本与 Hibernate 6 兼容

### B4. TypeScript
- [ ] `tsconfig.json` 中 `strict: true`
- [ ] `noUnusedLocals: true`, `noUnusedParameters: true`
- [ ] `npx vue-tsc --noEmit` 无错误
- [ ] API 层所有函数有明确的请求/响应类型
- [ ] `any` 类型使用 < 5 处

### B5. 前端代码
- [ ] `formatDate` 函数仅在 `utils/format.ts` 中定义
- [ ] `getStatusColor`/`getStatusText` 仅在工具文件中定义
- [ ] `.btn` 样式仅在公共 CSS 中定义
- [ ] `ApiResponse<T>` 和 `PageResult<T>` 类型统一
- [ ] Axios 响应拦截器统一返回格式
- [ ] 所有 API 模块使用 `@/utils/request` 导入
- [ ] 无未使用的文件（Empty.vue, HomePage.vue, store/index.ts）
- [ ] 无未使用的依赖（flowise-components, lucide-vue-next）

---

## C. 性能验收

### C1. 前端性能
- [ ] `dist/assets/` 中存在多个 chunk 文件（非单一 JS）
- [ ] 首屏加载 JS 体积 < 500KB（gzip 后）
- [ ] 路由懒加载：所有页面使用 `() => import()`
- [ ] Ant Design Vue 按需导入（非全量）
- [ ] Vite `manualChunks` 配置存在
- [ ] 构建产物包含 `.gz` 预压缩文件

### C2. 后端性能
- [ ] 所有列表接口支持分页参数（page, size）
- [ ] Agent 配置查询有 Redis 缓存（TTL 5min）
- [ ] 权限数据有 Redis 缓存（TTL 10min）
- [ ] 异步任务状态存储在 Redis 中（非内存）
- [ ] 应用重启后可查询异步任务状态
- [ ] 数据库连接池配置了 `leak-detection-threshold`

### C3. 数据库性能
- [ ] `agent_versions` 表有 `(agent_id, version_number)` 复合索引
- [ ] `agent_approvals` 表有 `(agent_id, status)` 复合索引
- [ ] `system_logs` 表有 `(module, created_at)` 复合索引
- [ ] SQL Migration 无重复的 `create_tenant_schema` 函数
- [ ] 数据库中只有一个 `tenants` 表

### C4. 基础设施性能
- [ ] Nginx 启用了 gzip 压缩
- [ ] Docker 日志配置了大小限制（max-size: 10m, max-file: 3）

---

## D. UI/UX 验收

### D1. OA 布局
- [ ] 顶部导航栏: Logo + 搜索框 + 通知 + 暗色切换 + 语言切换 + 用户菜单
- [ ] 左侧菜单: 支持展开/收起，有动画过渡
- [ ] 左侧菜单: 菜单分组正确
- [ ] 左侧菜单: 当前选中项有视觉指示
- [ ] 主内容区: 面包屑导航 + 页面标题 + 内容区
- [ ] 底部状态栏: 系统状态 + 版本信息
- [ ] 顶栏和侧边栏有毛玻璃效果（backdrop-blur）

### D2. 设计系统
- [ ] Tailwind 配置中定义了完整的颜色 Token（primary, neutral, success, warning, danger）
- [ ] 圆角统一（xl: 12px, 2xl: 16px）
- [ ] 阴影统一（glass, card, float）
- [ ] 动画定义完整（fade-in, slide-up, slide-in-left, scale-in）

### D3. 暗色模式
- [ ] 暗色模式切换按钮可用
- [ ] 切换后顶栏、侧边栏、内容区都正确适配暗色
- [ ] Ant Design 组件跟随暗色模式
- [ ] 暗色模式偏好持久化到 localStorage
- [ ] 系统偏好变化时自动跟随

### D4. 动画与过渡
- [ ] 路由切换有过渡动画（fadeIn + slideUp）
- [ ] 侧边栏展开/收起有动画（300ms）
- [ ] 卡片 hover 有上浮 + 阴影效果
- [ ] 按钮 hover/active 有状态变化
- [ ] 模态框有进入/退出动画

### D5. 页面设计
- [ ] 登录页: 左右分栏（大屏）/ 居中卡片（小屏），品牌插图
- [ ] Dashboard: 统计卡片有渐变背景 + 趋势指示
- [ ] 列表页面: 统一的头部（标题+搜索+操作）+ 表格 + 分页
- [ ] 卡片组件: 圆角 16px + 微妙阴影 + hover 效果
- [ ] 表单页面: 居中卡片 + 浮动标签 + 实时校验

### D6. 空状态与加载
- [ ] 所有列表页面有空状态展示
- [ ] Dashboard 有骨架屏加载
- [ ] 列表表格有骨架屏加载
- [ ] 空状态组件支持多种类型（无数据/搜索无结果/网络错误）

### D7. 响应式
- [ ] < 768px: 侧边栏变为抽屉式
- [ ] 768-1024px: 侧边栏默认收起
- [ ] > 1024px: 侧边栏默认展开
- [ ] 表格支持横向滚动
- [ ] 卡片网格自适应列数

### D8. 国际化
- [ ] 所有页面文本使用 `$t()` 或 `t()` 函数
- [ ] 无硬编码中文字符串
- [ ] 无硬编码英文字符串（除技术术语）
- [ ] 中英文切换正常
- [ ] 缺失的 i18n key 已补全

### D9. 样式一致性
- [ ] 主要使用 Tailwind CSS 进行布局和样式
- [ ] Ant Design 通过 Config Provider 自定义主题
- [ ] 无三套样式方案混用问题
- [ ] `corePlugins: { preflight: false }` 已配置

---

## E. 数据库验收

- [ ] Flyway migration 无重复函数定义
- [ ] 只有一个 `tenants` 表
- [ ] `users` 表 username 唯一约束为 `(username, tenant_id)` 组合
- [ ] 所有外键引用的表名正确
- [ ] 关键复合索引已创建
- [ ] `PageResult` 包含 page, size, totalPages 元数据
- [ ] JDBC URL 无 MySQL 参数

---

## F. 部署验收

### F1. Docker
- [ ] `backend/Dockerfile` 存在且多阶段构建正确
- [ ] `Dockerfile.frontend` 存在且多阶段构建正确
- [ ] `.dockerignore` 存在
- [ ] `docker-compose build` 成功
- [ ] `docker-compose up` 所有服务健康启动

### F2. Kubernetes
- [ ] K8s YAML 中无明文密码
- [ ] Ingress 配置了 `/api` 路由到 backend
- [ ] Ingress 配置了 TLS
- [ ] 后端配置了 `startupProbe`
- [ ] 后端配置了 `podAntiAffinity`
- [ ] PodDisruptionBudget 已配置

### F3. 监控
- [ ] Prometheus 告警规则文件存在
- [ ] Grafana Dashboard 配置存在
- [ ] Redis/PostgreSQL Exporter 已配置
- [ ] Actuator 端点需要 ADMIN 权限访问
- [ ] `show-details: when-authorized`

### F4. 其他
- [ ] `mise.toml` 包含 Node.js 版本
- [ ] `index.html` lang="zh-CN"，title="AI Agent Platform"
- [ ] CI/CD 流水线配置存在（如适用）

---

## G. 回归测试

- [ ] 用户注册/登录流程正常
- [ ] Agent 创建/编辑/删除/发布流程正常
- [ ] 审批提交/通过/驳回流程正常
- [ ] 测试用例创建/执行/查看结果流程正常
- [ ] 多租户隔离正常（不同租户数据不可见）
- [ ] 角色权限分配正常
- [ ] API Key 调用正常
- [ ] 国际化切换正常
- [ ] 暗色模式切换正常
- [ ] 所有页面无控制台错误
