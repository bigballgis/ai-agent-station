/**
 * 业务服务层。
 *
 * <p>本包包含所有核心业务逻辑的服务类，是应用程序的核心层。Service 层负责事务管理、
 * 业务规则校验、数据组装以及与其他服务的协调。所有写操作方法均标注
 * {@code @Transactional(rollbackFor = Exception.class)}。</p>
 *
 * <h3>子包</h3>
 * <ul>
 *   <li>{@code llm/} -- LLM 提供商集成（OpenAI、Qwen、Ollama）、LangChain4j 服务、ChatMemory 管理</li>
 *   <li>{@code tool/} -- 工具提供者（Function Calling、MCP 桥接、内置工具、自动注册）</li>
 * </ul>
 *
 * <h3>关键服务列表</h3>
 * <table>
 *   <tr><th>服务</th><th>职责</th></tr>
 *   <tr><td>{@code AgentService}</td><td>Agent CRUD、版本管理、模板市场、导入导出</td></tr>
 *   <tr><td>{@code WorkflowService}</td><td>工作流定义 CRUD、发布、版本管理</td></tr>
 *   <tr><td>{@code WorkflowEngine}</td><td>工作流实例执行引擎（DAG 图执行）</td></tr>
 *   <tr><td>{@code AuthService}</td><td>登录、注册、Token 管理、密码策略</td></tr>
 *   <tr><td>{@code UserService}</td><td>用户 CRUD、密码重置</td></tr>
 *   <tr><td>{@code RoleService}</td><td>角色 CRUD、用户角色分配</td></tr>
 *   <tr><td>{@code PermissionService}</td><td>权限 CRUD、角色权限分配</td></tr>
 *   <tr><td>{@code TenantService}</td><td>多租户 CRUD、配额管理、生命周期管理</td></tr>
 *   <tr><td>{@code QuotaService}</td><td>配额检查与更新</td></tr>
 *   <tr><td>{@code ApiInterfaceService}</td><td>API 接口管理、版本控制</td></tr>
 *   <tr><td>{@code AlertService}</td><td>告警规则 CRUD、告警触发与分发</td></tr>
 *   <tr><td>{@code FileStorageService}</td><td>文件上传、下载、删除</td></tr>
 *   <tr><td>{@code MemoryService}</td><td>Agent 记忆管理（短期/长期/业务记忆）</td></tr>
 *   <tr><td>{@code StreamService}</td><td>SSE 流式对话与 Agent 执行</td></tr>
 *   <tr><td>{@code NotificationService}</td><td>WebSocket 事件推送</td></tr>
 *   <tr><td>{@code DashboardService}</td><td>仪表盘统计数据聚合</td></tr>
 *   <tr><td>{@code CacheService}</td><td>缓存操作封装</td></tr>
 *   <tr><td>{@code CacheStatisticsService}</td><td>缓存命中率统计</td></tr>
 *   <tr><td>{@code CacheWarmUpService}</td><td>缓存预热</td></tr>
 *   <tr><td>{@code RateLimitService}</td><td>分布式速率限制（Redis）</td></tr>
 *   <tr><td>{@code RateLimitDashboardService}</td><td>速率限制仪表盘统计</td></tr>
 *   <tr><td>{@code SessionService}</td><td>用户会话管理</td></tr>
 *   <tr><td>{@code LoginLogService}</td><td>登录日志记录</td></tr>
 *   <tr><td>{@code SystemLogService}</td><td>系统日志查询</td></tr>
 *   <tr><td>{@code DataExportService}</td><td>数据导出（CSV，带记录数限制）</td></tr>
 *   <tr><td>{@code DataRetentionPolicyService}</td><td>数据保留策略执行</td></tr>
 *   <tr><td>{@code DeploymentService}</td><td>Agent 部署与发布</td></tr>
 *   <tr><td>{@code DictService}</td><td>数据字典管理</td></tr>
 *   <tr><td>{@code SuggestionService}</td><td>进化建议管理</td></tr>
 *   <tr><td>{@code ExperienceService}</td><td>进化经验管理</td></tr>
 *   <tr><td>{@code AgentExecutionMonitor}</td><td>Agent 执行并发控制与监控</td></tr>
 *   <tr><td>{@code ApplicationHealthService}</td><td>6 组件健康检查</td></tr>
 *   <tr><td>{@code AgentApprovalService}</td><td>审批流程管理</td></tr>
 *   <tr><td>{@code ApprovalChainService}</td><td>审批链配置</td></tr>
 *   <tr><td>{@code WorkflowExecutionRecovery}</td><td>工作流执行恢复</td></tr>
 *   <tr><td>{@code WorkflowAsyncExecutor}</td><td>工作流异步执行器</td></tr>
 *   <tr><td>{@code PermissionMatrixService}</td><td>权限矩阵管理</td></tr>
 *   <tr><td>{@code ReflectionEvaluationService}</td><td>反思评估服务</td></tr>
 *   <tr><td>{@code LlmResponseCacheService}</td><td>LLM 响应缓存</td></tr>
 *   <tr><td>{@code ApiCallLogService}</td><td>API 调用日志记录与查询</td></tr>
 *   <tr><td>{@code ApiCallAuditLogService}</td><td>API 调用审计日志（异步持久化）</td></tr>
 *   <tr><td>{@code LoginRateLimitService}</td><td>登录速率限制</td></tr>
 *   <tr><td>{@code TestDataCleanupService}</td><td>测试数据清理</td></tr>
 *   <tr><td>{@code UserDataService}</td><td>用户数据管理</td></tr>
 * </table>
 *
 * @see com.aiagent.controller REST 控制器层
 * @see com.aiagent.repository 数据访问层
 */
package com.aiagent.service;
