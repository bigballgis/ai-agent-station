# AI Agent Station — 整改实施方案与执行细则

> 版本：v1.0  
> 生成日期：2026-04-23  
> 适用范围：当前仓库的前后端集成修复、安全收口、DTO 改造、类型治理、测试补齐与文档同步  
> 目标读者：执行型 AI、初中级开发者、代码审查者、项目负责人

## 配套文档

本整改方案配套以下规范性文档一并使用：

1. `docs/AI_EXECUTION_SPEC.md` — 面向执行 AI / 审查 AI / 联调 AI 的行为规范、输出格式和任务分发标准
2. `docs/RECTIFICATION_ACCEPTANCE_CHECKLIST.md` — 面向实施完成后的自检、复检、阶段验收和里程碑评审的勾选式清单

---

## 1. 文档目的

本文件不是高层概念文档，而是一份**可直接执行的整改作战手册**。

设计目标：

1. 让能力一般的 AI 也能按顺序完成开发
2. 把“思路”转换为“可执行任务”
3. 限制无关改动，降低失控风险
4. 每个任务都具备明确的输入、修改范围、输出和验收标准
5. 优先恢复联调能力，再推进结构优化

---

## 2. 当前项目问题摘要

基于最新复审，当前问题可分为四大类：

### 2.1 前后端接口契约断裂

典型表现：
- 后端控制器大多已采用 `/v1/*`
- 前端仍混用 `/auth/*`、`/users`、`/agents`、`/api/agents`
- SSE `agent` 流式接口前端与后端方法不一致

直接风险：
- 登录失败
- 登出失败
- 刷新 token 失败
- 获取用户信息失败
- SSE 调试功能不可用
- 大量请求 404 / 405 / 401

### 2.2 前端响应处理模型不统一

典型表现：
- 请求层有时返回 `AxiosResponse`
- 业务层有时按 `res.code`
- 有时按 `res.data.data`
- 有时通过 `as any` 绕过类型系统

直接风险：
- 同一个接口在不同模块表现不一致
- 运行时错误难以定位
- 后续 AI 容易继续写错

### 2.3 登录态与租户态管理不一致

典型表现：
- 用户 store 已开始支持 `localStorage` / `sessionStorage`
- 请求层、SSE、部分 API 仍只读 `localStorage`
- 普通请求仍在由前端显式传 `X-Tenant-ID`

直接风险：
- “记住我”关闭时，后续请求丢 token
- 租户边界复杂化
- 前后端对租户来源理解不一致

### 2.4 DTO 改造不完整

典型表现：
- `UserController` 已完成部分 DTO 化
- 但测试、内存、经验、建议、租户等模块仍直接接收或返回 Entity

直接风险：
- 敏感字段暴露风险持续存在
- API 结构不稳定
- 实体变动会直接影响接口
- 后续重构成本继续累积

---

## 3. 整改总目标

整改目标分两层：

### 3.1 第一目标：恢复可联调、可运行、可验证

必须尽快恢复以下主链路：
- 登录
- 登出
- token 刷新
- 当前用户信息获取
- Agent 列表请求
- SSE 调试链路

### 3.2 第二目标：恢复工程稳定性与后续可维护性

在主链路恢复后，再推进：
- DTO 收口
- 类型统一
- 分页与通用返回统一
- 测试补齐
- 文档同步

---

## 4. 执行总原则

所有执行者必须遵守以下规则。

### 4.1 先修契约，再修结构

优先顺序：
1. 前后端路径一致
2. 响应结构一致
3. token 与租户读取一致
4. 再做 DTO / 类型 / 测试 / 文档

### 4.2 一次只做一个任务包

禁止同时大改：
- API 路径
- DTO 体系
- 类型系统
- 权限体系
- 页面样式

### 4.3 先读再改

每个任务开始前，必须阅读：
1. 本任务指定文件
2. 调用链上下游文件
3. 相关测试文件
4. 相关配置文件

### 4.4 不允许顺手重构

除非任务明确要求，否则禁止：
- 大规模格式化
- 目录结构迁移
- 全局命名改造
- 替换框架或基础库
- UI 美化
- 额外抽象层重构

### 4.5 每完成一个任务包，必须输出 5 项结果

1. 改了哪些文件
2. 改了什么
3. 为什么这么改
4. 还剩哪些风险
5. 是否通过验收标准

### 4.6 未通过验收，不进入下一个任务

如果任务验收失败，必须先修复，不得继续推进。

---

## 5. 总体路线图

建议分四个阶段推进。

| 阶段 | 目标 | 优先级 |
|------|------|--------|
| Phase 1 | 恢复联调能力 | P0 |
| Phase 2 | 完成安全与边界收口 | P1 |
| Phase 3 | 完成 DTO 与接口规范化 | P1 |
| Phase 4 | 完成类型治理、测试与文档同步 | P2 |

---

## 6. Phase 1：恢复联调能力（P0）

本阶段目标：**让项目重新具备可登录、可请求、可调试的能力**。

### 6.1 任务包 P0-1：统一前后端 API 路径

#### 目标
让前端所有请求路径与后端 Controller 路径完全一致。

#### 当前问题
后端控制器普遍为 `/v1/*`，但前端仍混用：
- `/auth/*`
- `/users`
- `/agents`
- `/api/agents`

#### 涉及文件
后端优先检查：
- `backend/src/main/java/com/aiagent/controller/AuthController.java`
- `backend/src/main/java/com/aiagent/controller/AgentController.java`
- `backend/src/main/java/com/aiagent/controller/RoleController.java`
- `backend/src/main/java/com/aiagent/controller/PermissionController.java`
- `backend/src/main/java/com/aiagent/controller/TenantController.java`
- 其他所有 `@RequestMapping("/v1/...`)` 控制器

前端优先修改：
- `frontend/src/api/user.ts`
- `frontend/src/api/agent.ts`
- `frontend/src/api/permission.ts`
- `frontend/src/api/tenant.ts`
- `frontend/src/api/log.ts`
- `frontend/src/api/test.ts`
- `frontend/src/store/modules/user.ts`
- `frontend/src/store/modules/agent.ts`
- `frontend/src/store/modules/workflow.ts`
- `frontend/src/store/modules/dict.ts`
- `frontend/src/store/modules/permission.ts`
- 所有仍直接调用 `request.*()` 的页面或 store

#### 修改规则
1. `request.ts` 的 `BASE_URL` 已经是 `/api`
2. 前端业务请求路径必须统一为 `/v1/...`
3. 禁止再出现以下字面量：
   - `/auth/...`
   - `/users`
   - `/agents`
   - `/roles`
   - `/permissions`
   - `/tenants`
   - `/api/...` 作为业务请求路径
4. 最终应类似：
   - `/v1/auth/login`
   - `/v1/auth/logout`
   - `/v1/auth/refresh`
   - `/v1/users`
   - `/v1/agents`
   - `/v1/roles`
   - `/v1/permissions`
   - `/v1/tenants`

#### 完成标准
- 前端源码中不再存在旧业务路径字面量
- 所有 API 模块与 store 请求路径对齐后端控制器
- 不再出现 `/api/xxx` 这种与 `BASE_URL` 重叠的双前缀写法

#### 禁止事项
- 不改业务逻辑
- 不顺便改返回结构
- 只处理路径一致性

#### 常见坑
- 某些 store 直接调 `request`，没经过 `api` 层
- 页面内部有散落的请求调用
- 路由路径 `/api/manage` 与接口路径无关，不要误改

---

### 6.2 任务包 P0-2：统一前端响应处理模型

#### 目标
前端全局只保留一种响应使用方式。

#### 推荐标准
统一规则：
- `request.ts` 成功响应统一返回业务包体 `ApiResponse<T>`
- 调用方统一按：
  - `res.code`
  - `res.message`
  - `res.data`

不允许再让业务层同时兼容 `AxiosResponse` 与业务包体两种模型。

#### 涉及文件
核心：
- `frontend/src/utils/request.ts`

联动修复：
- `frontend/src/store/modules/user.ts`
- `frontend/src/store/modules/agent.ts`
- `frontend/src/store/modules/workflow.ts`
- `frontend/src/store/modules/dict.ts`
- `frontend/src/store/modules/permission.ts`
- `frontend/src/store/modules/notification.ts`
- 其他仍用 `res.data.data` 的模块

#### 修改规则
1. 成功拦截器统一返回 `response.data`
2. 所有业务层代码禁止再写：
   - `res.data.data`
   - `res.data.code`
   - `const res = await request... as any` 后再猜结构
3. 分页数据统一写成：
   - `res.data.content`
   - 或根据统一分页结构处理
4. 将 `ApiResponse<T>` 泛型贯穿 API 层与 store 层

#### 完成标准
- `request.ts` 只返回一种成功结构
- `store` 和 `api` 不再混用 `AxiosResponse` 风格
- 不再出现主链路里大量 `as any` 读取响应

#### 禁止事项
- 不要同时做大规模 `any` 清理
- 先统一协议，再做类型美化

#### 常见坑
- 某些 API 函数已经自行包了一层返回
- 分页结构内部字段不统一，先统一外层协议，再处理内层分页模型

---

### 6.3 任务包 P0-3：统一 token / refreshToken / userInfo 存储读取策略

#### 目标
保证“记住我”开启或关闭时，请求链都能正确读取认证信息。

#### 当前问题
- 用户 store 已支持 `localStorage` / `sessionStorage`
- 请求层与 SSE 仍只读 `localStorage`
- `refreshToken` 与 `userInfo` 读取策略不一致

#### 涉及文件
- `frontend/src/store/modules/user.ts`
- `frontend/src/utils/request.ts`
- `frontend/src/api/agent.ts`
- `frontend/src/api/stream.ts`

建议新增：
- `frontend/src/utils/authStorage.ts`

#### 修改规则
1. 新建统一工具函数：
   - `getStoredToken()`
   - `getStoredRefreshToken()`
   - `getStoredUserInfo()`
2. 读取策略统一：
   - 先读 `localStorage`
   - 为空再读 `sessionStorage`
3. 所有业务代码禁止直接散落读取 `localStorage.getItem('token')`
4. `logout()` 必须同时清空：
   - localStorage
   - sessionStorage
5. 刷新 token 与用户信息读取都必须走统一工具

#### 完成标准
- “记住我=false” 时，请求链仍可携带 token
- 全项目 token 读取逻辑统一
- SSE、普通请求、刷新 token 行为一致

#### 常见坑
- 只修了登录，不修刷新 token
- API 层修了，但 SSE 没修
- `userInfo` 与 `token` 的存储策略不一致

---

### 6.4 任务包 P0-4：修复 SSE 前后端方法契约

#### 目标
让调试页与流式接口重新可用。

#### 当前问题
- `streamChat` 采用 POST 风格
- `streamAgentExecution` 前端采用 POST/fetch body 风格
- 后端 `agent` SSE 实际还是 GET

#### 涉及文件
- `backend/src/main/java/com/aiagent/controller/StreamController.java`
- `frontend/src/api/stream.ts`
- `frontend/src/pages/AgentDebugger.vue`

#### 推荐方案
建议统一为 **POST**：
- `POST /v1/stream/chat`
- `POST /v1/stream/agent/{agentId}`

原因：
- 参数更清晰
- 与复杂 body 更匹配
- 与当前前端 fetch body 风格一致

#### 修改规则
1. 后端新增或统一 `POST /v1/stream/agent/{agentId}`
2. 前端 `stream.ts` 与 `AgentDebugger.vue` 全部改为使用同一契约
3. 保留：
   - `Authorization`
   - `Accept: text/event-stream`
   - 正确的 body 结构
4. 如果保留 GET 仅为兼容，必须明确标注兼容用途，不得让前端主链路继续混用

#### 完成标准
- 调试页可收到 SSE 流
- 不再出现 404 / 405
- `onMessage` 能拿到节点事件或 token 事件

#### 常见坑
- 只改了前端，不改后端
- 只加了新接口，前端还连旧接口
- 鉴权头丢失
- `Content-Type` 或 `Accept` 处理错误

---

### 6.5 任务包 P0-5：修正旧路径残留配置

#### 目标
把框架配置中的旧认证路径残留一起清理掉。

#### 当前问题
旧认证路径仍可能残留在：
- `WebMvcConfig`
- 拦截器排除项
- 安全白名单

#### 涉及文件
- `backend/src/main/java/com/aiagent/config/WebMvcConfig.java`
- `backend/src/main/java/com/aiagent/security/SecurityConfig.java`
- 其他存在 `/auth/**` 残留的配置文件

#### 修改规则
1. 所有旧排除路径 `/auth/**` 改为 `/v1/auth/**`
2. 所有白名单与排除项使用最终真实路径
3. 如果为了兼容保留旧路径，必须有明确注释，且前端主链路不得再使用旧路径

#### 完成标准
- 拦截器排除与认证放行规则全部对齐 `/v1/auth/**`
- 不再出现旧路径残留配置

---

## 7. Phase 2：完成安全与边界收口（P1）

### 7.1 任务包 P1-1：收口租户传递规则

#### 目标
普通用户请求以 JWT 的租户信息为准，不再由前端默认显式传 `X-Tenant-ID`。

#### 涉及文件
- `backend/src/main/java/com/aiagent/tenant/TenantInterceptor.java`
- `frontend/src/utils/request.ts`
- `frontend/src/api/agent.ts`
- `frontend/src/api/stream.ts`

#### 修改规则
1. 普通 JWT 用户请求默认不传 `X-Tenant-ID`
2. 后端统一从 JWT 获取 tenant
3. 如果某些管理员场景需要切换租户，必须设计独立机制
4. API Key 调用场景单独讨论，不与普通 JWT 请求混用

#### 完成标准
- 普通用户前端不再默认注入 `X-Tenant-ID`
- 后端仍能正确构建租户上下文
- 越权租户请求仍被拒绝

---

### 7.2 任务包 P1-2：补齐认证相关接口契约

#### 目标
形成完整认证闭环：登录、刷新、登出、获取当前用户信息。

#### 当前问题
前端可能仍依赖当前用户信息接口，但后端不一定已补齐。

#### 涉及文件
- `backend/src/main/java/com/aiagent/controller/AuthController.java`
- `frontend/src/store/modules/user.ts`
- `frontend/src/api/user.ts`

#### 修改规则
二选一：
1. 后端补 `GET /v1/auth/userinfo`
2. 或前端删除对应依赖，完全使用登录响应恢复用户态

建议采用方案 1。

#### 完成标准
- 登录后可拿到用户信息
- 页面刷新后能恢复用户态
- `getUserInfo()` 不再调用不存在的接口

---

### 7.3 任务包 P1-3：清理默认凭据与环境变量策略

#### 目标
明确区分示例配置与真实运行配置。

#### 涉及文件
- `docker-compose.yml`
- `.env.example`
- `.env.production.example`
- `.env.staging.example`
- `k8s/secret.yml`
- `backend/src/main/resources/application.yml`

#### 修改规则
1. 生产关键密钥改为必须显式传入
2. 开发示例值只放到 `.env.example`
3. `docker-compose.yml` 不保留弱默认密码作为运行默认值
4. `k8s/secret.yml` 可保留占位值，但必须明确开发占位用途

#### 完成标准
- 运行配置中不再包含可直接用于生产的弱默认值
- 示例配置清晰、可读、可复制

---

### 7.4 任务包 P1-4：完成安全配置与拦截器一致性修复

#### 目标
让安全配置、拦截器配置、控制器路径三者完全一致。

#### 涉及文件
- `backend/src/main/java/com/aiagent/security/SecurityConfig.java`
- `backend/src/main/java/com/aiagent/config/WebMvcConfig.java`
- `backend/src/main/java/com/aiagent/controller/AuthController.java`
- `backend/src/main/java/com/aiagent/controller/StreamController.java`

#### 修改规则
1. 白名单路径只写最终路径
2. 刷新 token 接口不要被不合理权限模型卡死
3. 登出接口与认证上下文模型保持一致
4. SSE 鉴权方式要明确

#### 完成标准
- 登录、刷新、登出、SSE 权限模型自洽
- 不存在循环依赖式权限设计

---

## 8. Phase 3：完成 DTO 与接口规范化（P1）

### 8.1 任务包 P1-5：测试模块 DTO 化

#### 优先改造范围
- `backend/src/main/java/com/aiagent/controller/AgentTestCaseController.java`
- `backend/src/main/java/com/aiagent/controller/AgentTestExecutionController.java`
- `backend/src/main/java/com/aiagent/controller/AgentTestResultController.java`

#### 改造规则
1. 每个模块至少创建：
   - Request DTO
   - Response DTO
2. Controller 不再直接接收 Entity
3. Controller 不再直接返回 Entity
4. 统一通过转换器进行映射

#### 完成标准
- 不再出现 `@RequestBody Entity`
- 不再出现 `Result<Entity>` 或 `Result<List<Entity>>`

---

### 8.2 任务包 P1-6：租户 / 内存 / 建议 / 经验模块 DTO 化

#### 优先改造范围
- `backend/src/main/java/com/aiagent/controller/TenantController.java`
- `backend/src/main/java/com/aiagent/controller/MemoryController.java`
- `backend/src/main/java/com/aiagent/controller/SuggestionController.java`
- `backend/src/main/java/com/aiagent/controller/ExperienceController.java`

#### 完成标准
同 P1-5。

---

### 8.3 任务包 P1-7：统一分页与通用返回结构

#### 当前问题
前端存在重复且不一致的：
- `ApiResponse`
- `PageResult`

#### 涉及文件
- `frontend/src/types/api.ts`
- `frontend/src/types/common.ts`
- 所有使用分页结构的 API / store / 页面

#### 修改规则
1. 只保留一份 `ApiResponse`
2. 只保留一份 `PageResult`
3. 如果后端分页是：
   - `content`
   - `totalElements`
   - `totalPages`
   - `page`
   - `size`
   则前端统一使用这一份模型
4. 若存在历史兼容格式，必须在 API 层转换，不要把混乱传播到页面层

#### 完成标准
- 不再存在重复定义
- 页面不再需要猜测分页结构

---

## 9. Phase 4：完成类型治理、测试与文档同步（P2）

### 9.1 任务包 P2-1：合并重复类型定义

#### 涉及文件
- `frontend/src/types/api.ts`
- `frontend/src/types/common.ts`

#### 修改规则
1. 只保留一套通用类型
2. 统一 import 来源
3. 删除旧重复定义

#### 完成标准
- 全项目只保留一套 `ApiResponse`
- 全项目只保留一套 `PageResult`

---

### 9.2 任务包 P2-2：逐层清理 any

#### 优先顺序
1. API 层
2. Store 层
3. 高频页面
4. 测试代码最后处理

#### 当前高风险区域
- `frontend/src/store/modules/user.ts`
- `frontend/src/pages/AgentDebugger.vue`
- `frontend/src/pages/ApiManagement.vue`
- `frontend/src/pages/LogCenter.vue`
- 其他高频业务页

#### 修改规则
1. 主链路先去掉 `as any`
2. 对复杂对象先引入最小必要接口
3. 不要求一次清零，但要优先清主链路

#### 完成标准
- 核心 store 不再依赖 `any`
- 核心 API 函数具备明确返回类型

---

### 9.3 任务包 P2-3：补关键测试

#### 必补测试清单
1. 认证路径正确性
2. 请求拦截器返回结构一致性
3. session/local 双存储读取逻辑
4. SSE 调用方式正确性
5. DTO 转换正确性

#### 推荐文件
- `frontend/src/utils/__tests__/request.test.ts`
- `frontend/src/store/__tests__/user.test.ts`
- `backend/src/test/java/com/aiagent/controller/AuthControllerTest.java`
- 新增 DTOConverter 测试
- 新增 StreamController 契约测试

#### 完成标准
- 关键链路有自动化保护
- 后续修改更不容易回归

---

### 9.4 任务包 P2-4：文档同步

#### 涉及文件
- `README.md`
- `docs/spec.md`
- `docs/tasks.md`
- `docs/checklist.md`

#### 要同步的内容
- API 路径规范
- 认证接口说明
- token 存储策略
- SSE 使用方式
- DTO 改造范围
- 环境变量与密钥策略

#### 完成标准
- 文档与代码实际行为一致
- 新接手的 AI 不会被旧文档误导

---

## 10. 推荐执行顺序

以下顺序是给能力较弱 AI 的最稳执行顺序：

1. P0-1 统一 API 路径
2. P0-2 统一前端响应模型
3. P0-3 统一 token / refreshToken / userInfo 读取
4. P0-4 修复 SSE 契约
5. P0-5 修正旧路径残留配置
6. P1-2 补齐认证接口契约
7. P1-1 收口租户传递
8. P1-4 修安全配置一致性
9. P1-5 测试模块 DTO 化
10. P1-6 租户 / 内存 / 建议 / 经验 DTO 化
11. P1-7 统一分页返回
12. P2-1 合并重复类型
13. P2-2 清理核心 `any`
14. P2-3 补关键测试
15. P2-4 文档同步

---

## 11. 里程碑验收标准

### 11.1 里程碑 M1：联调恢复

满足以下全部条件才算通过：
- 登录成功
- 登出成功
- token 刷新成功
- 获取当前用户信息成功
- Agent 列表接口能正常返回
- AgentDebugger 能收到 SSE 事件流

### 11.2 里程碑 M2：安全边界恢复

满足以下全部条件才算通过：
- 普通请求不再依赖前端默认传租户头
- JWT 租户与请求租户冲突时被拒绝
- 不再存在旧认证路径残留配置
- 默认密钥与弱口令不再作为运行默认值

### 11.3 里程碑 M3：接口结构合格

满足以下全部条件才算通过：
- 用户、角色、权限、测试模块不再直出实体
- 前端只保留一套 `ApiResponse`
- 前端只保留一套 `PageResult`

### 11.4 里程碑 M4：可维护性达标

满足以下全部条件才算通过：
- 核心 store 不再依赖响应模型猜测
- 核心链路具备基本自动化测试
- 文档与代码一致

---

## 12. 给执行型 AI 的统一执行提示词模板

以下模板可直接复制给其他 AI：

### 模板 A：单任务执行模板

你现在只能完成一个任务包，不要跨任务扩散。

必须遵守：
1. 先阅读任务涉及的所有文件
2. 只修改本任务明确要求的文件
3. 不做无关重构
4. 改完后输出：
   - 修改文件列表
   - 修改摘要
   - 风险点
   - 验收结果
5. 如果发现上游设计冲突，先停止并报告，不要自行扩大修改范围

修改标准：
- 优先保证联调可用
- 优先保证前后端契约一致
- 优先减少隐式行为
- 保持现有代码风格

完成后必须核对：
- 是否影响其他 API 路径
- 是否影响 token 读取
- 是否影响权限路径
- 是否影响 SSE
- 是否引入新的重复类型定义

### 模板 B：修复结果回报模板

请按以下格式输出：

1. 本次任务包编号：
2. 修改文件：
3. 修改内容摘要：
4. 为什么这样改：
5. 可能影响的上下游模块：
6. 已完成的验收项：
7. 未解决风险：
8. 是否建议进入下一任务：是 / 否

---

## 13. 给执行型 AI 的验收清单表格

| 任务包 | 核心验收点 | 必须通过 |
|--------|------------|----------|
| P0-1 | 前端请求路径全部改为 `/v1/*` | 是 |
| P0-2 | 请求层只返回一种响应结构 | 是 |
| P0-3 | token / refreshToken / userInfo 读取统一 | 是 |
| P0-4 | SSE `agent` 链路可用 | 是 |
| P0-5 | 无旧 `/auth/**` 配置残留 | 是 |
| P1-1 | 普通请求默认不再前端传租户头 | 是 |
| P1-2 | `userinfo` 闭环成立 | 是 |
| P1-3 | 默认密钥与弱口令收口 | 是 |
| P1-4 | 安全配置与拦截器一致 | 是 |
| P1-5 | 测试模块 DTO 化完成 | 是 |
| P1-6 | 其他重点模块 DTO 化完成 | 是 |
| P1-7 | 分页与通用返回统一 | 是 |
| P2-1 | 重复类型定义已删除 | 是 |
| P2-2 | 核心链路 `any` 明显收缩 | 否 |
| P2-3 | 关键测试覆盖主链路 | 否 |
| P2-4 | 文档与实现一致 | 否 |

---

## 14. 明确禁止事项

以下动作禁止在本轮整改中进行：

1. 不要更换请求库
2. 不要把 Pinia 改成别的状态管理
3. 不要把 Spring MVC 改成 WebFlux
4. 不要改数据库结构来解决接口问题
5. 不要重做权限系统
6. 不要顺手改 UI 和样式
7. 不要在一轮里同时做：
   - 全局 DTO 化
   - 全局 `any` 清理
   - 全局国际化修复
   - 全局权限改造

原因：
- 低能力 AI 极易失控
- 任务边界会迅速模糊
- 回归风险极高

---

## 15. 第一批必须优先完成的 5 项

如果资源有限，必须优先完成下面 5 项：

1. 统一前端所有请求路径到 `/v1/*`
2. 统一 `request.ts` 返回业务包体，不再返回 `AxiosResponse`
3. 统一 token / refreshToken / userInfo 的双存储读取
4. 统一 SSE `agent` 接口的前后端调用方式
5. 修正认证与拦截器中的旧路径残留

只要这 5 项做对，项目会先从“当前不稳定集成态”回到“可持续开发态”。

---

## 16. 建议的整改交付物

整改完成后，建议至少产出以下交付物：

1. 代码修改结果
2. 更新后的测试报告
3. 一份最终的联调通过清单
4. 更新后的文档集合
5. 一份剩余问题清单（如有）

---

## 17. 最终目标分数

如果严格按本方案执行，预计项目可达到：

- 联调恢复后：75~78 分
- 安全与边界收口后：80~82 分
- DTO 与类型收口后：83~85 分

---

## 18. 结语

本整改方案的核心思想只有一句话：

> 先恢复系统一致性，再推进结构优雅性。

不要试图一步到位，不要让低能力 AI 同时处理多种问题。  
把任务边界切清、验收规则写死，项目就能稳步修回正轨。
