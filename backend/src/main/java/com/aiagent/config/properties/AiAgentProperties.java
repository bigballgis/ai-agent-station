package com.aiagent.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AI Agent 主应用配置属性
 *
 * 对应 application.yml 中 ai-agent.* 前缀的配置项。
 * 使用 @ConfigurationProperties 实现类型安全的配置绑定。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai-agent")
public class AiAgentProperties {

    /** 安全配置 */
    private Security security = new Security();

    /** 会话配置 */
    private Session session = new Session();

    /** 速率限制配置 */
    private RateLimit rateLimit = new RateLimit();

    /** 告警配置 */
    private Alert alert = new Alert();

    /** 缓存配置 */
    private Cache cache = new Cache();

    /** 大模型配置 */
    private Llm llm = new Llm();

    /** LangChain4j 配置 */
    private Langchain4j langchain4j = new Langchain4j();

    /** Agent 编排配置 */
    private Orchestration orchestration = new Orchestration();

    /** Agent 执行配置 */
    private Execution execution = new Execution();

    /** 租户配置 */
    private Tenant tenant = new Tenant();

    /** 内存配置 */
    private Memory memory = new Memory();

    /** 测试配置 */
    private Test test = new Test();

    /** 加密配置 */
    private Crypto crypto = new Crypto();

    /**
     * MCP（Model Context Protocol）客户端 — 与远端 MCP Server 协商协议版本与传输参数。
     */
    private Mcp mcp = new Mcp();

    @Data
    public static class Security {
        /** 最大登录失败次数 */
        @Min(1)
        @Max(20)
        private int maxFailedAttempts = 5;

        /** 账户锁定时长（分钟） */
        @Min(1)
        @Max(1440)
        private int lockoutDurationMinutes = 30;

        /** 密码历史记录数量 */
        @Min(0)
        @Max(24)
        private int passwordHistoryCount = 5;

        /** 刷新令牌有效期（天） */
        @Min(1)
        @Max(365)
        private long refreshTokenTtlDays = 7;
    }

    @Data
    public static class Session {
        /** 会话超时时间（小时） */
        @Min(1)
        @Max(720)
        private long timeoutHours = 24;

        /** 最大并发会话数 */
        @Min(1)
        @Max(10)
        private int maxConcurrentSessions = 3;
    }

    @Data
    public static class RateLimit {
        /** 每分钟最大登录尝试次数 */
        @Min(1)
        @Max(60)
        private int maxAttemptsPerMinute = 5;

        /** 每IP全局每分钟最大尝试次数 */
        @Min(1)
        @Max(100)
        private int maxIpGlobalPerMinute = 10;

        /** 速率限制窗口（秒） */
        @Min(1)
        @Max(3600)
        private int windowSeconds = 60;

        /** 基础退避时间（秒） */
        @Min(1)
        @Max(60)
        private int baseBackoffSeconds = 2;

        /** API 速率限制配置 */
        private ApiRateLimit api = new ApiRateLimit();

        @Data
        public static class ApiRateLimit {
            /** 每分钟默认 API 请求限制 */
            @Min(1)
            @Max(10000)
            private int defaultLimitPerMinute = 100;

            /** 每小时默认 API 请求限制 */
            @Min(1)
            @Max(100000)
            private int defaultLimitPerHour = 1000;

            /** 突发容量 */
            @Min(1)
            @Max(1000)
            private int burstCapacity = 20;
        }
    }

    @Data
    public static class Alert {
        /** Webhook 最大重试次数 */
        @Min(0)
        @Max(10)
        private int webhookMaxRetries = 3;
    }

    @Data
    public static class Cache {
        /** 默认缓存 TTL（分钟） */
        @Min(1)
        @Max(1440)
        private int defaultTtlMinutes = 30;

        /** 字典缓存 TTL（小时） */
        @Min(1)
        @Max(168)
        private int dictTtlHours = 2;

        /** 权限缓存 TTL（分钟） */
        @Min(1)
        @Max(1440)
        private int permissionTtlMinutes = 30;

        /** LLM 响应缓存 TTL（分钟） */
        @Min(1)
        @Max(1440)
        private int llmResponseTtlMinutes = 30;
    }

    @Data
    public static class Llm {
        /** 默认 LLM 提供者 */
        @NotBlank
        private String defaultProvider = "openai";

        /** OpenAI 配置 */
        private ProviderConfig openai = new ProviderConfig(
                "", "https://api.openai.com/v1", "gpt-4o", 60, false, false);

        /** 通义千问配置 */
        private ProviderConfig qwen = new ProviderConfig(
                "", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", 60, false, false);

        /** Ollama 配置 */
        private OllamaConfig ollama = new OllamaConfig();

        @Data
        public static class ProviderConfig {
            private String apiKey = "";
            private String baseUrl;
            private String defaultModel;
            @Min(1)
            @Max(600)
            private Integer timeoutSeconds = 60;
            private Boolean logRequests = false;
            private Boolean logResponses = false;

            public ProviderConfig() {}

            public ProviderConfig(String apiKey, String baseUrl, String defaultModel,
                                  Integer timeoutSeconds, Boolean logRequests, Boolean logResponses) {
                this.apiKey = apiKey;
                this.baseUrl = baseUrl;
                this.defaultModel = defaultModel;
                this.timeoutSeconds = timeoutSeconds;
                this.logRequests = logRequests;
                this.logResponses = logResponses;
            }
        }

        @Data
        public static class OllamaConfig {
            private String baseUrl = "http://localhost:11434";
            private String defaultModel = "qwen2:7b";
            @Min(1)
            @Max(600)
            private Integer timeoutSeconds = 120;
            @Min(0)
            private Integer gpuLayers = 0;
            @Min(512)
            @Max(131072)
            private Integer numCtx = 4096;
        }
    }

    @Data
    public static class Langchain4j {
        private ChatMemory chatMemory = new ChatMemory();
        private ToolCalling toolCalling = new ToolCalling();

        @Data
        public static class ChatMemory {
            /** 最大消息数 */
            @Min(1)
            @Max(200)
            private int maxMessages = 20;

            /** 存储类型: in-memory / redis */
            @NotBlank
            private String storeType = "in-memory";
        }

        @Data
        public static class ToolCalling {
            /** 最大迭代次数 */
            @Min(1)
            @Max(50)
            private int maxIterations = 5;

            /** 是否启用工具调用 */
            private boolean enabled = true;
        }
    }

    @Data
    public static class Orchestration {
        private String engine = "langchain4j";
        private String timeout = "30s";
    }

    @Data
    public static class Execution {
        /** 每个 Agent 最大并发执行数 */
        @Min(1)
        @Max(100)
        private int maxConcurrentPerAgent = 5;

        /** 执行超时时间（秒） */
        @Min(1)
        @Max(3600)
        private int timeoutSeconds = 120;
    }

    @Data
    public static class Tenant {
        /** 默认 schema */
        @NotBlank
        private String defaultSchema = "public";

        /** schema 前缀 */
        @NotBlank
        private String schemaPrefix = "t_";

        /** 默认管理员密码 */
        private String defaultAdminPassword = "Admin@123456";
    }

    @Data
    public static class Memory {
        private boolean enabled = true;
        @Min(1)
        private int maxSessions = 10000;
        private String sessionTtl = "86400s";
    }

    @Data
    public static class Test {
        private DataIsolation dataIsolation = new DataIsolation();
        private TestExecution execution = new TestExecution();
        private TestCleanup cleanup = new TestCleanup();

        @Data
        public static class DataIsolation {
            private boolean enabled = true;
            private String schemaPrefix = "test_";
        }

        @Data
        public static class TestExecution {
            private String timeout = "60s";
            @Min(1)
            private int concurrentLimit = 10;
            @Min(0)
            private int maxRetry = 3;
        }

        @Data
        public static class TestCleanup {
            private boolean enabled = true;
            @Min(1)
            private int retentionDays = 30;
            private String cronExpression = "0 0 0 * * ?";
        }
    }

    @Data
    public static class Crypto {
        /** 加密密钥 */
        private String secretKey = "";
    }

    /**
     * MCP 工具调用：协议版本字符串与官方规范按日期发布的修订对齐（非「MCP2 产品代号」）。
     */
    @Data
    public static class Mcp {
        /**
         * initialize 请求中的 protocolVersion，须与目标 MCP Server 支持的规范版本一致。
         * 常见值如 2024-11-05、2025-03-26、2025-11-25 等，以 modelcontextprotocol.io 当前文档为准。
         */
        @NotBlank
        private String protocolVersion = "2025-11-25";

        @NotBlank
        private String clientName = "AegisNexus";

        @NotBlank
        private String clientVersion = "1.0.0";

        @Min(1000)
        @Max(120_000)
        private int connectTimeoutMs = 10_000;

        @Min(1000)
        @Max(300_000)
        private int readTimeoutMs = 60_000;

        /**
         * initialize 成功后发送 {@code notifications/initialized}（规范推荐；极简实现可关闭）。
         */
        private boolean sendInitializedNotification = true;
    }
}
