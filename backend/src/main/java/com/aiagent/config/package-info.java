/**
 * Spring 配置类。
 *
 * <p>本包包含应用程序的所有 Spring 配置类，涵盖安全、缓存、数据库、监控、
 * API 文档、WebSocket、线程池、CORS、限流等基础设施配置。</p>
 *
 * <h3>配置类列表</h3>
 * <table>
 *   <tr><th>配置类</th><th>职责</th></tr>
 *   <tr><td>{@code OpenApiConfig}</td><td>SpringDoc OpenAPI 配置（Swagger UI、API 分组、安全方案）</td></tr>
 *   <tr><td>{@code SecurityConfig}</td><td>Spring Security 配置（JWT 过滤器链、公开/受保护端点）</td></tr>
 *   <tr><td>{@code CorsConfig}</td><td>CORS 跨域配置（环境变量驱动，生产环境严格限制）</td></tr>
 *   <tr><td>{@code RedisConfig}</td><td>Redis 连接配置（序列化、连接池）</td></tr>
 *   <tr><td>{@code CacheConfig}</td><td>缓存管理器配置（RedisCacheManager、多区域 TTL）</td></tr>
 *   <tr><td>{@code CacheStatisticsAspect}</td><td>缓存统计 AOP（命中/未命中追踪）</td></tr>
 *   <tr><td>{@code JpaConfig}</td><td>JPA/Hibernate 配置（审计、软删除）</td></tr>
 *   <tr><td>{@code AsyncConfig}</td><td>异步线程池配置（核心 5、最大 20、队列 100）</td></tr>
 *   <tr><td>{@code ThreadPoolConfig}</td><td>通用线程池配置</td></tr>
 *   <tr><td>{@code WebSocketConfig}</td><td>WebSocket 端点与处理器配置</td></tr>
 *   <tr><td>{@code WebMvcConfig}</td><td>Web MVC 配置（拦截器、资源处理）</td></tr>
 *   <tr><td>{@code TraceFilter}</td><td>请求追踪过滤器（MDC traceId、响应时间）</td></tr>
 *   <tr><td>{@code TenantContextFilter}</td><td>租户上下文过滤器（MDC tenantId、userId）</td></tr>
 *   <tr><td>{@code RequestResponseLoggingFilter}</td><td>请求/响应日志过滤器（敏感字段遮蔽）</td></tr>
 *   <tr><td>{@code ETagFilterConfig}</td><td>ETag 缓存过滤器（MD5 哈希、304 响应）</td></tr>
 *   <tr><td>{@code SecurityHeadersConfig}</td><td>安全响应头配置（CSP、HSTS、COOP、COEP）</td></tr>
 *   <tr><td>{@code FileUploadConfig}</td><td>文件上传配置（大小限制、存储路径）</td></tr>
 *   <tr><td>{@code MetricsConfig}</td><td>Prometheus 指标配置</td></tr>
 *   <tr><td>{@code ApiResponseTimeMetrics}</td><td>API 响应时间 Prometheus 直方图</td></tr>
 *   <tr><td>{@code HealthIndicatorConfig}</td><td>健康检查指示器（DB、Redis、磁盘）</td></tr>
 *   <tr><td>{@code StartupHealthValidator}</td><td>启动时健康校验</td></tr>
 *   <tr><td>{@code StartupLogConfig}</td><td>启动日志配置（活跃配置项输出）</td></tr>
 *   <tr><td>{@code MessageSourceConfig}</td><td>i18n 消息源配置（messages.properties）</td></tr>
 *   <tr><td>{@code DataRetentionConfig}</td><td>数据保留策略配置</td></tr>
 *   <tr><td>{@code RetryConfig}</td><td>Spring Retry 配置</td></tr>
 *   <tr><td>{@code ResilienceConfig}</td><td>Resilience4j 配置（熔断、重试、限流）</td></tr>
 *   <tr><td>{@code ApiVersionConfig}</td><td>API 版本控制配置（X-API-Version 头）</td></tr>
 *   <tr><td>{@code ApiVersionFilter}</td><td>API 版本过滤器</td></tr>
 *   <tr><td>{@code ApiVersionRequestMappingHandlerMapping}</td><td>API 版本路由映射</td></tr>
 * </table>
 *
 * @see com.aiagent.security 安全框架配置
 * @see com.aiagent.service 业务服务层
 */
package com.aiagent.config;
