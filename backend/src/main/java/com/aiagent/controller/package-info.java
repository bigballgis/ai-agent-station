/**
 * REST API 控制器层。
 *
 * <p>本包包含所有 HTTP 端点的控制器类，负责接收请求、参数校验、调用 Service 层并返回统一响应。
 * 所有控制器使用 {@code /v1/*} 路由前缀，通过 DTO/VO 进行数据传输，不直接暴露 Entity。</p>
 *
 * <h3>关键设计原则</h3>
 * <ul>
 *   <li>Controller 层仅做参数校验和响应封装，业务逻辑下沉到 Service 层</li>
 *   <li>所有入参使用 {@code @Valid} 校验，返回 {@code Result<T>} 统一响应</li>
 *   <li>权限控制通过 {@code @RequiresRole} / {@code @RequiresPermission} 注解实现</li>
 *   <li>API 文档通过 {@code @Parameter}、{@code @ApiResponse}、{@code @Schema} 注解生成</li>
 * </ul>
 *
 * <h3>控制器列表</h3>
 * <table>
 *   <tr><th>控制器</th><th>路径前缀</th><th>职责</th></tr>
 *   <tr><td>{@code AuthController}</td><td>/v1/auth</td><td>登录、注册、Token 管理、验证码、密码</td></tr>
 *   <tr><td>{@code AgentController}</td><td>/v1/agents</td><td>Agent CRUD、版本、模板、导入导出</td></tr>
 *   <tr><td>{@code AgentApiController}</td><td>/v1/agent</td><td>Agent 调用执行、状态查询</td></tr>
 *   <tr><td>{@code WorkflowController}</td><td>/v1/workflows</td><td>工作流定义、实例、审批</td></tr>
 *   <tr><td>{@code ApiInterfaceController}</td><td>/v1/api-interfaces</td><td>API 接口管理、版本控制</td></tr>
 *   <tr><td>{@code ApiCallLogController}</td><td>/v1/api-call-logs</td><td>API 调用日志查询与统计</td></tr>
 *   <tr><td>{@code ToolController}</td><td>/v1/tools</td><td>MCP 工具与 Function Calling 管理</td></tr>
 *   <tr><td>{@code FileController}</td><td>/v1/files</td><td>文件上传、下载、列表</td></tr>
 *   <tr><td>{@code UserController}</td><td>/v1/users</td><td>用户 CRUD、密码重置</td></tr>
 *   <tr><td>{@code RoleController}</td><td>/v1/roles</td><td>角色 CRUD、角色分配</td></tr>
 *   <tr><td>{@code PermissionController}</td><td>/v1/permissions</td><td>权限 CRUD、权限分配</td></tr>
 *   <tr><td>{@code TenantController}</td><td>/v1/tenants</td><td>多租户 CRUD、API 密钥</td></tr>
 *   <tr><td>{@code QuotaController}</td><td>/v1/quotas</td><td>配额查询与更新</td></tr>
 *   <tr><td>{@code AlertController}</td><td>/v1/alerts</td><td>告警规则与记录管理</td></tr>
 *   <tr><td>{@code AgentApprovalController}</td><td>/v1/approvals</td><td>审批提交与审批操作</td></tr>
 *   <tr><td>{@code StreamController}</td><td>/v1/stream</td><td>SSE 流式对话与 Agent 执行</td></tr>
 *   <tr><td>{@code SessionController}</td><td>/v1/sessions</td><td>会话管理与踢出</td></tr>
 *   <tr><td>{@code DashboardController}</td><td>/v1/dashboard</td><td>仪表盘统计数据</td></tr>
 *   <tr><td>{@code MemoryController}</td><td>/v1/memory</td><td>Agent 记忆管理</td></tr>
 *   <tr><td>{@code DeploymentController}</td><td>/v1/deployments</td><td>Agent 部署与发布</td></tr>
 *   <tr><td>{@code DictController}</td><td>/v1/dicts</td><td>数据字典管理</td></tr>
 *   <tr><td>{@code CacheStatsController}</td><td>/v1/cache-stats</td><td>缓存统计信息</td></tr>
 *   <tr><td>{@code RateLimitController}</td><td>/v1/rate-limits</td><td>速率限制查询与仪表盘</td></tr>
 *   <tr><td>{@code DataExportController}</td><td>/v1/data-export</td><td>数据导出（CSV）</td></tr>
 *   <tr><td>{@code SystemLogController}</td><td>/v1/system-logs</td><td>系统日志查询</td></tr>
 *   <tr><td>{@code SuggestionController}</td><td>/v1/suggestions</td><td>进化建议管理</td></tr>
 *   <tr><td>{@code ExperienceController}</td><td>/v1/experiences</td><td>进化经验管理</td></tr>
 *   <tr><td>{@code ExecutionHistoryController}</td><td>/v1/executions</td><td>执行历史查询</td></tr>
 *   <tr><td>{@code HealthDetailController}</td><td>/v1/health</td><td>健康检查详情</td></tr>
 *   <tr><td>{@code ApiChangelogController}</td><td>/v1/api-changelog</td><td>API 变更日志查询</td></tr>
 *   <tr><td>{@code UserDataController}</td><td>/v1/user-data</td><td>用户数据管理</td></tr>
 *   <tr><td>{@code AgentTestCaseController}</td><td>/v1/agent-tests</td><td>Agent 测试用例管理</td></tr>
 *   <tr><td>{@code AgentTestExecutionController}</td><td>/v1/agent-test-executions</td><td>Agent 测试执行管理</td></tr>
 *   <tr><td>{@code AgentTestResultController}</td><td>/v1/agent-test-results</td><td>Agent 测试结果管理</td></tr>
 * </table>
 *
 * @see com.aiagent.service 业务服务层
 * @see com.aiagent.dto 数据传输对象
 * @see com.aiagent.vo 视图对象
 * @see com.aiagent.common.Result 统一响应包装
 */
package com.aiagent.controller;
