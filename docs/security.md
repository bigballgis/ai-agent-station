# 安全措施文档

> 版本: v1.0 | 最后更新: Round 279

---

## 1. 认证安全

### 1.1 JWT 双 Token 机制

| Token 类型 | 有效期 | 用途 |
|-----------|--------|------|
| Access Token | 30 分钟（可配置） | API 请求认证 |
| Refresh Token | 7 天（可配置） | 刷新 Access Token |

**实现细节**:
- 签名算法: HS256（显式指定）
- 密钥要求: 至少 32 字符，通过环境变量 `JWT_SECRET` 注入
- Claims: sub（用户ID）、iss（签发者）、aud（受众）、iat（签发时间）、exp（过期时间）
- 时钟偏移: 60 秒容差
- Token 黑名单: 登出时 Access Token 加入 Redis 黑名单
- Refresh Token 轮换: 刷新时生成新 Refresh Token，旧 Token 失效

### 1.2 API Key 认证

- 通过请求头 `X-API-Key` 传递
- API Key 使用 AES-256-GCM 加密存储
- 租户级 API Key，支持重新生成

### 1.3 账户安全

| 措施 | 参数 |
|------|------|
| 密码哈希 | BCrypt，强度 12 |
| 密码策略 | 最少 8 位，含大小写字母和数字 |
| 密码历史 | 最近 5 次不可重复 |
| 登录锁定 | 5 次失败后锁定 30 分钟 |
| 会话限制 | 最多 3 个并发设备 |
| 验证码 | 数学验证码（登录时启用） |

---

## 2. 授权安全

### 2.1 RBAC 权限模型

```
用户 (User)
  └── 角色 (Role) [多对多]
       └── 权限 (Permission) [多对多]
            └── 资源操作 (resource:action)
```

### 2.2 权限注解

- `@RequiresRole("ADMIN")` -- 角色级别控制
- `@RequiresPermission("agent:create")` -- 操作级别控制
- `@Audited` -- 操作审计记录

### 2.3 数据隔离

- 15 个 Repository 实现租户安全查询
- 所有查询自动添加 `tenant_id` 过滤条件
- 配额强制执行（Agent 数、工作流数、存储空间）

---

## 3. 传输安全

### 3.1 TLS

- Nginx 处理 TLS 终止
- 证书存放: `docker/nginx/ssl/`
- 推荐: Let's Encrypt 自动续期

### 3.2 安全响应头

| 头 | 值 | 说明 |
|---|---|------|
| Content-Security-Policy | 10 个指令（OWASP Level 2） | 防止 XSS、点击劫持 |
| Strict-Transport-Security | max-age=31536000 | 强制 HTTPS |
| X-Content-Type-Options | nosniff | 防止 MIME 嗅探 |
| X-Frame-Options | SAMEORIGIN | 防止点击劫持 |
| X-XSS-Protection | 0（依赖 CSP） | XSS 防护 |
| Cross-Origin-Opener-Policy | same-origin | 防止跨域攻击 |
| Cross-Origin-Embedder-Policy | require-corp | 防止跨域嵌入 |
| Permissions-Policy | 扩展配置 | 限制浏览器 API |
| Referrer-Policy | strict-origin-when-cross-origin | 控制 Referer |

### 3.3 CORS

- 环境变量驱动: `CORS_ALLOWED_ORIGINS`
- 生产环境: 仅允许指定域名，禁止通配符
- 凭证支持: `allowCredentials = true`
- 预检缓存: 3600 秒

---

## 4. 输入安全

### 4.1 SQL 注入防护

- 所有数据库查询使用参数化查询（JPA @Query）
- 排序字段通过 `SortFieldValidator` 白名单校验
- 分页参数: size 最大 100，防御性边界检查

### 4.2 XSS 防护

- CSP 策略: `script-src 'self'`、`object-src 'none'`
- 前端: DOMPurify 清洗 v-html 内容
- 输出编码: Vue 默认转义

### 4.3 Prompt 注入防御

- `PromptInjectionFilter`: 检测并清洗恶意 Prompt 输入
- 正则匹配已知攻击模式

### 4.4 路径遍历防护

- `FileController`: subDir 参数校验，禁止 `..` 和绝对路径
- `FileUploadValidator`: 文件类型白名单

### 4.5 请求大小限制

| 限制 | 值 |
|------|---|
| 文件上传 | 50 MB |
| 请求体 | 100 MB |

---

## 5. 数据安全

### 5.1 加密

| 数据 | 加密方式 | 说明 |
|------|---------|------|
| API Key | AES-256-GCM | 存储加密 |
| 密码 | BCrypt(12) | 单向哈希 |
| JWT Token | HS256 | 签名验证 |

### 5.2 敏感数据脱敏

- `@Sensitive` 注解标记敏感字段（11 个）
- `SensitiveDataSerializer`: 响应中自动脱敏
- `DataMaskingUtils`: 日志中自动遮蔽
- 遮蔽字段: password、token、apiKey、secret、accessToken 等

### 5.3 数据保留

- 可配置数据保留策略
- 租户隔离清理
- 审计日志保留

---

## 6. API 安全

### 6.1 速率限制

| 端点 | 限制 | 窗口 |
|------|------|------|
| 登录 | 10 次 | 5 分钟 |
| 密码重置 | 5 次 | 5 分钟 |
| 文件上传 | 20 次 | 1 分钟 |
| Agent 调用 | 30 次 | 1 分钟 |

- 实现: Redis INCR + EXPIRE 滑动窗口
- 响应头: X-RateLimit-Limit、X-RateLimit-Remaining、X-RateLimit-Reset
- 降级: Redis 不可用时使用内存限流

### 6.2 API 版本控制

- 请求头: `X-API-Version: 1|2`
- 废弃机制: `@ApiDeprecation` 注解
- 废弃响应头: Sunset、Deprecation、Warning、Link

### 6.3 Swagger 安全

- 生产环境: ADMIN 角色才能访问
- 可通过环境变量禁用
- Try-It-Out 功能已禁用

---

## 7. 审计与日志

### 7.1 操作审计

- `@Audited` 注解标记需要审计的方法（38 个）
- 自动记录: 操作者、操作类型、资源、IP、User-Agent
- 审计日志: `system_logs` 表

### 7.2 数据变更审计

- `DataChangeAuditAspect`: 自动记录数据变更
- 记录: 变更前值、变更后值、操作者、时间戳

### 7.3 登录审计

- 登录成功/失败记录
- 登录 IP、User-Agent
- 异常登录检测

### 7.4 日志安全

- 敏感参数自动遮蔽（password、token、apiKey 等）
- 请求体截断（10k 字符）
- 响应体截断（1000 字符）
- MDC: traceId、tenantId、userId

---

## 8. 基础设施安全

### 8.1 容器安全

- Docker 网络隔离（3 个网络）
- 端口绑定: 默认 127.0.0.1（不对外暴露）
- 资源限制: 所有服务设置内存/CPU 限制
- .dockerignore: 最小化镜像

### 8.2 密钥管理

- 所有密钥通过环境变量注入
- 不在镜像中硬编码任何密钥
- `.env.production.example` 包含安全警告

### 8.3 CI/CD 安全

- Trivy 容器镜像安全扫描
- JaCoCo 覆盖率检查
- 分支保护: main 分支需要 PR + Review
