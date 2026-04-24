package com.aiagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
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
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiKeyName = "apiKeyAuth";

        return new OpenAPI()
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
                        .title("AI Agent Platform API")
                        .version("1.0.0")
                        .description("""
                                ## 企业级低代码 AI Agent 平台 API 文档

                                ### 功能模块
                                - **认证管理**: 用户登录、注册、Token 管理
                                - **Agent 管理**: AI Agent 的 CRUD、测试、部署
                                - **工作流引擎**: 工作流定义、执行、监控
                                - **工具管理**: 外部工具集成配置
                                - **用户管理**: 用户、角色、权限管理
                                - **系统监控**: 日志、告警、配额管理

                                ### 认证方式
                                1. **JWT Bearer Token**: 登录后获取 Access Token，在请求头中携带 `Authorization: Bearer {token}`
                                2. **API Key**: 在请求头中携带 `X-API-Key: {your-api-key}`
                                """)
                        .contact(new Contact()
                                .name("AI Agent Team")
                                .email("support@aiagent.com")
                                .url("https://aiagent.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
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
                .pathsToMatch("/v1/agents/**")
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
                        "/v1/memory/**", "/v1/sessions/**", "/v1/executions/**")
                .build();
    }
}
