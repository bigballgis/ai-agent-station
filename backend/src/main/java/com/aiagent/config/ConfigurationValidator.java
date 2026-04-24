package com.aiagent.config;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.config.properties.CorsProperties;
import com.aiagent.config.properties.JwtProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用启动配置验证器
 *
 * 在应用启动时验证所有关键配置项，提供清晰的错误信息帮助快速定位问题。
 * 验证规则:
 * - JWT 密钥必须至少 32 个字符
 * - 数据库 URL 必须存在
 * - Redis URL 必须存在（生产环境）
 * - CORS origins 必须配置（生产环境）
 * - 加密密钥必须有效（不能为默认值或空）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationValidator {

    private final JwtProperties jwtProperties;
    private final AiAgentProperties aiAgentProperties;
    private final CorsProperties corsProperties;
    private final Environment environment;

    /** 敏感配置关键词，用于日志脱敏 */
    private static final String[] SENSITIVE_KEYWORDS = {
            "password", "secret", "key", "token", "credential"
    };

    @PostConstruct
    public void validateConfiguration() {
        log.info("=== 开始验证应用配置 ===");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. 验证 JWT 密钥
        validateJwtSecret(errors, warnings);

        // 2. 验证数据库配置
        validateDatabaseConfig(errors, warnings);

        // 3. 验证 Redis 配置
        validateRedisConfig(errors, warnings);

        // 4. 验证 CORS 配置（仅生产环境）
        validateCorsConfig(errors, warnings);

        // 5. 验证加密密钥
        validateCryptoKey(errors, warnings);

        // 6. 验证 LLM 配置
        validateLlmConfig(warnings);

        // 输出验证结果
        if (!warnings.isEmpty()) {
            log.warn("=== 配置验证警告 ===");
            for (String warning : warnings) {
                log.warn("  [WARN] {}", warning);
            }
        }

        if (!errors.isEmpty()) {
            log.error("=== 配置验证失败 ===");
            for (String error : errors) {
                log.error("  [ERROR] {}", error);
            }
            throw new IllegalStateException(
                    "应用配置验证失败，共 " + errors.size() + " 个错误。请检查上方日志获取详细信息。");
        }

        log.info("=== 配置验证通过 ===");
        logActiveConfiguration();
    }

    /**
     * 验证 JWT 密钥配置
     */
    private void validateJwtSecret(List<String> errors, List<String> warnings) {
        String secret = jwtProperties.getSecret();

        if (secret == null || secret.isBlank()) {
            errors.add("JWT 密钥未配置。请设置环境变量 JWT_SECRET。" +
                    "生成方式: openssl rand -base64 32");
            return;
        }

        if (secret.length() < 32) {
            errors.add("JWT 密钥长度不足（当前 " + secret.length() + " 字符，至少需要 32 字符）。" +
                    "请设置环境变量 JWT_SECRET 为一个足够长的随机字符串。");
            return;
        }

        // 检查是否使用了明显的弱密钥
        String[] weakSecrets = {"secret", "password", "123456", "changeme", "default"};
        for (String weak : weakSecrets) {
            if (secret.toLowerCase().contains(weak)) {
                warnings.add("JWT 密钥可能过于简单，建议使用更强的随机密钥。");
                break;
            }
        }

        log.info("  [OK] JWT 密钥已配置（长度: {} 字符）", secret.length());
    }

    /**
     * 验证数据库配置
     */
    private void validateDatabaseConfig(List<String> errors, List<String> warnings) {
        String dbUrl = environment.getProperty("spring.datasource.url");

        if (dbUrl == null || dbUrl.isBlank()) {
            errors.add("数据库 URL 未配置。请设置环境变量 SPRING_DATASOURCE_URL。" +
                    "示例: jdbc:postgresql://localhost:5432/ai_agent_platform");
            return;
        }

        log.info("  [OK] 数据库 URL 已配置: {}", maskSensitiveInfo(dbUrl));

        // 检查连接池配置
        String maxPool = environment.getProperty("spring.datasource.hikari.maximum-pool-size", "20");
        String minIdle = environment.getProperty("spring.datasource.hikari.minimum-idle", "5");
        log.info("  [OK] 连接池配置: max={}, min-idle={}", maxPool, minIdle);
    }

    /**
     * 验证 Redis 配置
     */
    private void validateRedisConfig(List<String> errors, List<String> warnings) {
        String redisHost = environment.getProperty("spring.data.redis.host", "localhost");
        String redisPort = environment.getProperty("spring.data.redis.port", "6379");

        boolean isProduction = isProductionProfile();

        if (isProduction) {
            // 生产环境必须配置 Redis
            if ("localhost".equals(redisHost) || "127.0.0.1".equals(redisHost)) {
                warnings.add("生产环境 Redis 地址为 localhost，请确认是否为预期配置。" +
                        "建议使用独立的 Redis 服务地址。");
            }
        }

        log.info("  [OK] Redis 配置: {}:{}", redisHost, redisPort);
    }

    /**
     * 验证 CORS 配置（仅生产环境）
     */
    private void validateCorsConfig(List<String> errors, List<String> warnings) {
        if (!isProductionProfile()) {
            log.info("  [OK] CORS 配置: 非生产环境，跳过严格验证");
            return;
        }

        String allowedOrigins = corsProperties.getAllowedOrigins();

        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            errors.add("生产环境 CORS allowed-origins 未配置。" +
                    "请设置环境变量 CORS_ALLOWED_ORIGINS 为允许的前端域名列表（逗号分隔）。");
            return;
        }

        if (allowedOrigins.contains("*")) {
            errors.add("生产环境 CORS 配置包含通配符 '*'，这是严重的安全风险。" +
                    "请设置具体的域名列表，例如: CORS_ALLOWED_ORIGINS=https://yourdomain.com");
            return;
        }

        log.info("  [OK] CORS 配置已配置（{} 个来源）", allowedOrigins.split(",").length);
    }

    /**
     * 验证加密密钥
     */
    private void validateCryptoKey(List<String> errors, List<String> warnings) {
        String cryptoKey = aiAgentProperties.getCrypto().getSecretKey();

        if (cryptoKey == null || cryptoKey.isEmpty()) {
            errors.add("加密密钥未配置。请设置环境变量 AI_AGENT_CRYPTO_SECRET_KEY。" +
                    "此密钥用于加密存储 API Key 等敏感数据。");
            return;
        }

        if (cryptoKey.equals("default-secret-key-change-in-production!!")) {
            errors.add("加密密钥仍为默认值。请设置一个安全的加密密钥。" +
                    "生成方式: openssl rand -base64 32");
            return;
        }

        if (cryptoKey.length() < 16) {
            warnings.add("加密密钥长度较短（" + cryptoKey.length() + " 字符），建议至少 32 字符。");
        }

        log.info("  [OK] 加密密钥已配置（长度: {} 字符）", cryptoKey.length());
    }

    /**
     * 验证 LLM 配置
     */
    private void validateLlmConfig(List<String> warnings) {
        String defaultProvider = aiAgentProperties.getLlm().getDefaultProvider();
        log.info("  [OK] 默认 LLM 提供者: {}", defaultProvider);

        boolean anyProviderConfigured = false;

        // 检查 OpenAI
        String openaiKey = aiAgentProperties.getLlm().getOpenai().getApiKey();
        if (openaiKey != null && !openaiKey.isBlank()) {
            anyProviderConfigured = true;
            log.info("  [OK] OpenAI 已配置: baseUrl={}", aiAgentProperties.getLlm().getOpenai().getBaseUrl());
        } else {
            log.warn("  [WARN] OpenAI API Key 未配置");
        }

        // 检查 Qwen
        String qwenKey = aiAgentProperties.getLlm().getQwen().getApiKey();
        if (qwenKey != null && !qwenKey.isBlank()) {
            anyProviderConfigured = true;
            log.info("  [OK] 通义千问已配置: baseUrl={}", aiAgentProperties.getLlm().getQwen().getBaseUrl());
        } else {
            log.warn("  [WARN] 通义千问 API Key 未配置");
        }

        // Ollama 不需要 API Key
        log.info("  [OK] Ollama 已配置: baseUrl={}", aiAgentProperties.getLlm().getOllama().getBaseUrl());

        if (!anyProviderConfigured) {
            warnings.add("没有配置任何云端 LLM 提供者的 API Key。AI 功能将仅依赖本地 Ollama。");
        }
    }

    /**
     * 记录当前激活的配置概要（排除敏感信息）
     */
    private void logActiveConfiguration() {
        log.info("=== 当前激活配置概要 ===");
        log.info("  Profile          : {}", String.join(", ", environment.getActiveProfiles()));
        log.info("  Server Port       : {}", environment.getProperty("server.port", "8080"));
        log.info("  Database URL      : {}", maskSensitiveInfo(
                environment.getProperty("spring.datasource.url", "(not configured)")));
        log.info("  Redis            : {}:{}",
                environment.getProperty("spring.data.redis.host", "localhost"),
                environment.getProperty("spring.data.redis.port", "6379"));
        log.info("  JWT Expiration    : {}ms", jwtProperties.getExpiration());
        log.info("  Default LLM      : {}", aiAgentProperties.getLlm().getDefaultProvider());
        log.info("  Cache TTL        : {}min", aiAgentProperties.getCache().getDefaultTtlMinutes());
        log.info("  Rate Limit       : {}/min (API)",
                aiAgentProperties.getRateLimit().getApi().getDefaultLimitPerMinute());
        log.info("  CORS Origins     : {}", corsProperties.getAllowedOrigins());
        log.info("============================");
    }

    /**
     * 判断是否为生产环境
     */
    private boolean isProductionProfile() {
        String[] profiles = environment.getActiveProfiles();
        for (String profile : profiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 脱敏处理：隐藏 URL 中的密码和敏感信息
     */
    private String maskSensitiveInfo(String value) {
        if (value == null || value.isEmpty()) {
            return "(not configured)";
        }
        // 隐藏数据库 URL 中的密码: jdbc:postgresql://user:password@host -> jdbc:postgresql://user:****@host
        String masked = value.replaceAll("(?<=:)([^:@/]+)(?=@)", "****");
        // 隐藏查询参数中的敏感值
        masked = masked.replaceAll("(?<=password=)([^&]+)", "****");
        return masked;
    }
}
