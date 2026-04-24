/**
 * 安全框架。
 *
 * <p>本包提供应用程序的安全基础设施，包括 JWT 认证、密码策略、输入校验、
 * 文件上传安全和 Prompt 注入防御。</p>
 *
 * <h3>核心组件</h3>
 * <table>
 *   <tr><th>类</th><th>职责</th></tr>
 *   <tr><td>{@code SecurityConfig}</td><td>Spring Security 主配置（过滤器链、端点权限规则）</td></tr>
 *   <tr><td>{@code JwtUtil}</td><td>JWT Token 生成、解析、验证（HS256、aud/iss/clockSkew）</td></tr>
 *   <tr><td>{@code JwtAuthenticationFilter}</td><td>JWT 认证过滤器（从请求头提取 Token 并验证）</td></tr>
 *   <tr><td>{@code JwtAuthenticationEntryPoint}</td><td>未认证请求处理器（返回 401）</td></tr>
 *   <tr><td>{@code UserPrincipal}</td><td>认证用户主体（实现 UserDetails）</td></tr>
 *   <tr><td>{@code ApiKeyService}</td><td>API Key 认证服务（X-API-Key 头验证）</td></tr>
 *   <tr><td>{@code PromptInjectionFilter}</td><td>Prompt 注入攻击防御（输入检测与清洗）</td></tr>
 * </table>
 *
 * <h3>子包</h3>
 * <ul>
 *   <li>{@code annotation/} -- 安全相关注解（{@code @Auditable}）</li>
 *   <li>{@code filter/} -- 文件上传安全过滤器（{@code FileUploadSecurityFilter}）</li>
 *   <li>{@code validator/} -- 安全校验器：
 *     <ul>
 *       <li>{@code PasswordPolicyValidator} -- 密码强度校验（长度、复杂度、历史密码）</li>
 *       <li>{@code SortFieldValidator} -- 排序字段白名单校验（防止 JPQL 注入）</li>
 *       <li>{@code FileUploadValidator} -- 文件上传校验（类型、大小、路径遍历）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>JWT 双 Token 认证（Access Token + Refresh Token）</li>
 *   <li>BCrypt(12) 密码哈希（金融级强度）</li>
 *   <li>账户锁定（5 次失败 / 30 分钟锁定）</li>
 *   <li>密码历史（最近 5 次不可重复使用）</li>
 *   <li>会话管理（最多 3 个并发设备）</li>
 *   <li>API Key 认证（X-API-Key 头）</li>
 *   <li>Prompt 注入防御</li>
 *   <li>文件上传安全（类型白名单、路径遍历防护）</li>
 *   <li>JPQL 注入防护（排序字段白名单）</li>
 * </ul>
 *
 * @see com.aiagent.config.SecurityConfig Spring Security 配置
 * @see com.aiagent.config.SecurityHeadersConfig 安全响应头配置
 * @see com.aiagent.aspect.RateLimitAspect 速率限制切面
 */
package com.aiagent.security;
