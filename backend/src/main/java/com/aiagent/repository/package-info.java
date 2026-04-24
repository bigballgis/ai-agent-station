/**
 * 数据访问层（Spring Data JPA Repository）。
 *
 * <p>本包包含所有 JPA Repository 接口，负责数据库访问。所有 Repository 均已实现
 * 租户安全查询（通过 {@code @Query} 注解自动添加 {@code tenant_id} 过滤条件），
 * 并标注 {@code @Transactional(readOnly = true)} 以优化只读查询性能。</p>
 *
 * <h3>关键设计原则</h3>
 * <ul>
 *   <li>所有查询方法均包含租户隔离条件，防止跨租户数据泄露</li>
 *   <li>分页查询使用 {@code @QueryHints} 设置 {@code HINT_FETCH_SIZE} 优化性能</li>
 *   <li>复杂查询使用 {@code @EntityGraph} 解决 N+1 问题</li>
 *   <li>实体支持软删除（通过 {@code @SQLDelete} + {@code @Where} 实现）</li>
 *   <li>排序字段通过 {@code SortFieldValidator} 白名单校验，防止 JPQL 注入</li>
 * </ul>
 *
 * <h3>Repository 列表</h3>
 * <table>
 *   <tr><th>Repository</th><th>实体</th><th>关键自定义方法</th></tr>
 *   <tr><td>{@code AgentRepository}</td><td>Agent</td><td>11 个自定义方法（按租户/状态/类别查询）</td></tr>
 *   <tr><td>{@code AgentVersionRepository}</td><td>AgentVersion</td><td>版本查询与回滚</td></tr>
 *   <tr><td>{@code AgentMemoryRepository}</td><td>AgentMemory</td><td>按 Agent/类型查询</td></tr>
 *   <tr><td>{@code AgentApprovalRepository}</td><td>AgentApproval</td><td>按状态/Agent 查询</td></tr>
 *   <tr><td>{@code AgentTestCaseRepository}</td><td>AgentTestCase</td><td>按 Agent 查询</td></tr>
 *   <tr><td>{@code AgentTestExecutionRepository}</td><td>AgentTestExecution</td><td>按用例/状态查询</td></tr>
 *   <tr><td>{@code AgentTestResultRepository}</td><td>AgentTestResult</td><td>按执行/状态查询</td></tr>
 *   <tr><td>{@code WorkflowDefinitionRepository}</td><td>WorkflowDefinition</td><td>7 个自定义方法（版本管理）</td></tr>
 *   <tr><td>{@code WorkflowInstanceRepository}</td><td>WorkflowInstance</td><td>按定义/状态查询</td></tr>
 *   <tr><td>{@code WorkflowNodeLogRepository}</td><td>WorkflowNodeLog</td><td>按实例查询</td></tr>
 *   <tr><td>{@code UserRepository}</td><td>User</td><td>10 个自定义方法（按用户名/邮箱/租户查询）</td></tr>
 *   <tr><td>{@code RoleRepository}</td><td>Role</td><td>按名称/租户查询</td></tr>
 *   <tr><td>{@code PermissionRepository}</td><td>Permission</td><td>按代码/模块查询</td></tr>
 *   <tr><td>{@code TenantRepository}</td><td>Tenant</td><td>按代码/域名查询</td></tr>
 *   <tr><td>{@code ApiInterfaceRepository}</td><td>ApiInterface</td><td>按 Agent/路径查询</td></tr>
 *   <tr><td>{@code ApiCallLogRepository}</td><td>ApiCallLog</td><td>按请求 ID/时间范围查询</td></tr>
 *   <tr><td>{@code ApiCallAuditLogRepository}</td><td>ApiCallAuditLog</td><td>审计日志查询</td></tr>
 *   <tr><td>{@code McpToolRepository}</td><td>McpTool</td><td>按名称/状态查询</td></tr>
 *   <tr><td>{@code AlertRuleRepository}</td><td>AlertRule</td><td>按租户/启用状态查询</td></tr>
 *   <tr><td>{@code AlertRecordRepository}</td><td>AlertRecord</td><td>按规则/状态查询</td></tr>
 *   <tr><td>{@code ExecutionHistoryRepository}</td><td>ExecutionHistory</td><td>按 Agent/时间范围查询</td></tr>
 *   <tr><td>{@code DictTypeRepository}</td><td>DictType</td><td>按代码查询</td></tr>
 *   <tr><td>{@code DictItemRepository}</td><td>DictItem</td><td>按类型查询</td></tr>
 *   <tr><td>{@code LoginLogRepository}</td><td>LoginLog</td><td>按用户/时间范围查询</td></tr>
 *   <tr><td>{@code SystemLogRepository}</td><td>SystemLog</td><td>按模块/级别查询</td></tr>
 *   <tr><td>{@code UserSessionRepository}</td><td>UserSession</td><td>按用户/Token 查询</td></tr>
 *   <tr><td>{@code PasswordHistoryRepository}</td><td>PasswordHistory</td><td>按用户查询（最近 N 条）</td></tr>
 *   <tr><td>{@code DataChangeLogRepository}</td><td>DataChangeLog</td><td>按资源/操作者查询</td></tr>
 *   <tr><td>{@code DeploymentHistoryRepository}</td><td>DeploymentHistory</td><td>按 Agent 查询</td></tr>
 *   <tr><td>{@code ApprovalChainRepository}</td><td>ApprovalChain</td><td>按 Agent 查询</td></tr>
 *   <tr><td>{@code ApprovalChainStepRepository}</td><td>ApprovalChainStep</td><td>按链查询</td></tr>
 *   <tr><td>{@code RateLimitConfigRepository}</td><td>RateLimitConfig</td><td>按端点查询</td></tr>
 *   <tr><td>{@code PermissionMatrixRepository}</td><td>PermissionMatrix</td><td>按角色/资源查询</td></tr>
 *   <tr><td>{@code UserRoleRepository}</td><td>UserRole</td><td>按用户/角色查询</td></tr>
 *   <tr><td>{@code RolePermissionRepository}</td><td>RolePermission</td><td>按角色查询</td></tr>
 *   <tr><td>{@code AgentEvolutionSuggestionRepository}</td><td>AgentEvolutionSuggestion</td><td>按 Agent/状态查询</td></tr>
 *   <tr><td>{@code AgentEvolutionExperienceRepository}</td><td>AgentEvolutionExperience</td><td>按 Agent 查询</td></tr>
 *   <tr><td>{@code AgentEvolutionReflectionRepository}</td><td>AgentEvolutionReflection</td><td>按 Agent 查询</td></tr>
 *   <tr><td>{@code McpToolCallLogRepository}</td><td>McpToolCallLog</td><td>按工具/时间查询</td></tr>
 * </table>
 *
 * @see com.aiagent.entity JPA 实体
 * @see com.aiagent.service 业务服务层
 */
package com.aiagent.repository;
