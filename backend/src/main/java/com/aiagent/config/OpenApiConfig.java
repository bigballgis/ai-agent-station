package com.aiagent.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiKeyName = "apiKeyAuth";

        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://staging-api.aiagent.com")
                                .description("预发布环境"),
                        new Server()
                                .url("https://api.aiagent.com")
                                .description("生产环境")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName).addList(apiKeyName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("输入 JWT Token，格式: Bearer {token}"))
                        .addSecuritySchemes(apiKeyName,
                                new SecurityScheme()
                                        .name(apiKeyName)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API Key 认证"))
                        // 通用错误响应示例
                        .addResponses("400", createErrorResponse("请求参数错误", "参数校验失败或请求格式不正确"))
                        .addResponses("401", createErrorResponse("未认证", "Token 已过期或未提供，请重新登录"))
                        .addResponses("403", createErrorResponse("无权限", "当前用户没有执行该操作的权限"))
                        .addResponses("404", createErrorResponse("资源不存在", "请求的资源未找到"))
                        .addResponses("429", createErrorResponse("请求过于频繁", "已触发速率限制，请稍后重试"))
                        .addResponses("500", createErrorResponse("服务器内部错误", "服务器处理请求时发生异常")))
                .info(new Info()
                        .title("AegisNexus Platform API")
                        .version("1.0.0")
                        .description("""
                                ## 企业级低代码 AI Agent 平台 API 文档

                                ### 功能模块
                                - **认证管理**: 用户登录、注册、Token 管理、验证码
                                - **Agent 管理**: AI Agent 的 CRUD、版本管理、模板市场、调用执行
                                - **工作流引擎**: 工作流定义、版本管理、实例执行、审批流转
                                - **API 网关**: API 接口管理、版本控制、调用日志与统计
                                - **工具管理**: MCP 工具集成、健康检查、Function Calling
                                - **文件管理**: 文件上传、下载、列表、删除
                                - **用户管理**: 用户、角色、权限管理
                                - **租户管理**: 多租户 CRUD、配额管理、API 密钥
                                - **系统监控**: 缓存统计、告警规则、健康检查

                                ### 认证方式
                                1. **JWT Bearer Token**: 登录后获取 Access Token，在请求头中携带 `Authorization: Bearer {token}`
                                2. **API Key**: 在请求头中携带 `X-API-Key: {your-api-key}`

                                ### API 版本
                                - 通过请求头 `X-API-Version: 1|2` 指定 API 版本（默认: 1）
                                - 响应头 `X-API-Version` 返回当前使用的版本号
                                """)
                        .contact(new Contact()
                                .name("AI Agent Team")
                                .email("support@aiagent.com")
                                .url("https://aiagent.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("AegisNexus 完整文档")
                        .url("https://github.com/aegisnexus/aegisnexus#readme"))
                .tags(List.of(
                        new Tag().name("认证管理").description("用户登录、注册、Token刷新、验证码、密码管理。支持 JWT Bearer Token 和 API Key 两种认证方式。"),
                        new Tag().name("Agent管理").description("AI Agent的CRUD、版本管理、模板市场、复制与评分。支持 DAG 图编排和多 LLM 集成。"),
                        new Tag().name("Agent API").description("Agent调用执行、状态查询、异步任务管理。支持同步/异步/流式三种调用模式。"),
                        new Tag().name("工作流管理").description("工作流定义CRUD、版本管理、实例执行、审批流转。支持 DAG 图执行引擎和节点超时控制。"),
                        new Tag().name("API接口管理").description("API接口CRUD、版本控制、废弃与启用切换。支持 Sunset/Warning/Link 废弃响应头。"),
                        new Tag().name("API调用日志").description("API调用日志查询、统计与分析。支持按时间范围、Agent、状态等维度聚合。"),
                        new Tag().name("工具管理").description("MCP工具与Function Calling工具查询、健康检查、连接测试。支持自动注册和健康监控。"),
                        new Tag().name("文件管理").description("文件上传、下载、列表、删除。支持路径隔离和大小限制（50MB）。"),
                        new Tag().name("用户管理").description("用户CRUD、密码重置。需要 ADMIN 角色权限。"),
                        new Tag().name("角色管理").description("角色CRUD、用户角色分配。支持 RBAC 权限模型。"),
                        new Tag().name("权限管理").description("权限CRUD、角色权限分配。权限粒度到资源操作级别。"),
                        new Tag().name("租户管理").description("多租户CRUD、API密钥管理、租户激活。需要 SUPER_ADMIN 角色权限。"),
                        new Tag().name("配额管理").description("租户配额查询与限制更新。支持 Agent、工作流、存储等资源配额。"),
                        new Tag().name("缓存统计").description("缓存命中率、命中/未命中次数统计。覆盖 9 个缓存区域。"),
                        new Tag().name("告警管理").description("告警规则CRUD、告警记录查询与处理。支持 Webhook/邮件/站内信多通道告警。"),
                        new Tag().name("部署管理").description("Agent部署与发布管理。支持环境隔离和版本回滚。"),
                        new Tag().name("监控与测试").description("Agent测试、会话管理、执行历史、数据导出。包含健康检查和 Prometheus 指标端点。"),
                        new Tag().name("审批管理").description("审批提交、审批通过、拒绝。支持多级审批链和工作流内嵌审批。"),
                        new Tag().name("流式对话").description("SSE 流式对话与 Agent 执行。支持 GET/POST 两种方式，事件格式包含 token/done/error/node_start/node_end。"),
                        new Tag().name("会话管理").description("在线会话查询、踢出、多设备管理。最多 3 个并发会话。"),
                        new Tag().name("速率限制").description("速率限制查询与仪表盘。支持 Redis 分布式限流和标准 RateLimit 响应头。"),
                        new Tag().name("数据导出").description("数据导出为 CSV 格式。支持 Agent、工作流等数据导出，带记录数限制。")
                ));
    }

    /**
     * 创建通用错误响应
     */
    private ApiResponse createErrorResponse(String message, String detail) {
        return new ApiResponse()
                .description(message)
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/Result"))
                                .example(Map.of(
                                        "code", 400,
                                        "message", message,
                                        "data", null
                                ))));
    }

    /**
     * 为所有操作自动添加通用错误响应
     */
    @Bean
    public OpenApiCustomizer globalResponseCustomizer() {
        return openApi -> {
            openApi.getPaths().forEach((path, pathItem) -> {
                Map<String, Operation> operations = Map.of(
                        "get", pathItem.getGet(),
                        "post", pathItem.getPost(),
                        "put", pathItem.getPut(),
                        "delete", pathItem.getDelete(),
                        "patch", pathItem.getPatch()
                );
                operations.forEach((method, operation) -> {
                    if (operation != null) {
                        ApiResponses responses = operation.getResponses();
                        if (responses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        }
                        // 为每个操作添加 401 和 500 响应（如果尚未定义）
                        if (!responses.containsKey("401")) {
                            responses.addApiResponse("401", new ApiResponse()
                                    .description("未认证 - Token 已过期或未提供")
                                    .content(new Content()
                                            .addMediaType("application/json", new MediaType()
                                                    .example(Map.of(
                                                            "code", 401,
                                                            "message", "未认证",
                                                            "data", null
                                                    )))));
                        }
                        if (!responses.containsKey("500")) {
                            responses.addApiResponse("500", new ApiResponse()
                                    .description("服务器内部错误")
                                    .content(new Content()
                                            .addMediaType("application/json", new MediaType()
                                                    .example(Map.of(
                                                            "code", 500,
                                                            "message", "服务器内部错误",
                                                            "data", null
                                                    )))));
                        }
                    }
                });
            });
        };
    }

    // ==================== API 分组 ====================

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证管理")
                .pathsToMatch("/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi agentApi() {
        return GroupedOpenApi.builder()
                .group("Agent管理")
                .pathsToMatch("/v1/agents/**", "/v1/agent/**", "/v1/task/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("工作流管理")
                .pathsToMatch("/v1/workflows/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理")
                .pathsToMatch("/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统管理")
                .pathsToMatch("/v1/roles/**", "/v1/permissions/**", "/v1/tenants/**",
                        "/v1/dicts/**", "/v1/system-logs/**", "/v1/alerts/**")
                .build();
    }

    @Bean
    public GroupedOpenApi toolApi() {
        return GroupedOpenApi.builder()
                .group("工具与接口")
                .pathsToMatch("/v1/tools/**", "/v1/api-interfaces/**", "/v1/api-call-logs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi deploymentApi() {
        return GroupedOpenApi.builder()
                .group("部署管理")
                .pathsToMatch("/v1/deployments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi monitoringApi() {
        return GroupedOpenApi.builder()
                .group("监控与测试")
                .pathsToMatch("/v1/agent-tests/**", "/v1/quotas/**", "/v1/data-export/**",
                        "/v1/memory/**", "/v1/sessions/**", "/v1/executions/**",
                        "/v1/cache-stats/**", "/v1/files/**")
                .build();
    }
}
