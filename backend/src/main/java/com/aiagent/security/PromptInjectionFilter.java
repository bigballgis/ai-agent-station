package com.aiagent.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt Injection 防护过滤器
 * 
 * 基于 OWASP AI Agent Security Cheat Sheet 最佳实践:
 * - "Use delimiters and clear boundaries between instructions and data"
 * - "Apply content filtering for known injection patterns"
 * - "Consider using separate LLM calls to validate/summarize"
 * 
 * 三层防御:
 * 1. 黑名单模式匹配 — 已知攻击模式（DFA 算法，高性能）
 * 2. 结构化边界检测 — 检测用户输入中是否包含指令覆盖尝试
 * 3. 内容安全过滤 — 敏感词/有害内容过滤
 */
@Slf4j
@Component
public class PromptInjectionFilter {

    /**
     * 已知 Prompt Injection 攻击模式（中英文）
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // 指令覆盖
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)"),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)"),
            Pattern.compile("(?i)override\\s+(the\\s+)?(system|previous)\\s+(instructions?|prompts?|rules?)"),
            Pattern.compile("(?i)new\\s+instructions?\\s*[:：]"),
            Pattern.compile("(?i)system\\s*[:：]\\s*you\\s+are"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+a"),
            Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)"),
            Pattern.compile("(?i)act\\s+as\\s+(if\\s+you\\s+are|a|an)"),
            Pattern.compile("(?i)roleplay\\s+as"),
            // 角色劫持
            Pattern.compile("(?i)\\bDAN\\b"),  // Do Anything Now
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)developer\\s+mode"),
            Pattern.compile("(?i)越狱"),
            Pattern.compile("(?i)忽略.*指令"),
            Pattern.compile("(?i)忘记.*指令"),
            Pattern.compile("(?i)你现在是"),
            Pattern.compile("(?i)假装你是"),
            // 分隔符注入
            Pattern.compile("(?i)<\\|im_start\\|>"),
            Pattern.compile("(?i)<\\|im_end\\|>"),
            Pattern.compile("(?i)\\[INST\\]"),
            Pattern.compile("(?i)\\[/INST\\]"),
            Pattern.compile("(?i)\\{\\{.*?\\}\\}.*?system"),
            // 输出操控
            Pattern.compile("(?i)output\\s+(the\\s+)?(following|above|this)"),
            Pattern.compile("(?i)print\\s+(the\\s+)?(following|above|this)"),
            Pattern.compile("(?i)reveal\\s+(the\\s+)?(system|hidden|secret)"),
            Pattern.compile("(?i)show\\s+(me\\s+)?(the\\s+)?(system|hidden|secret|prompt)"),
            Pattern.compile("(?i)泄露.*系统"),
            Pattern.compile("(?i)显示.*系统")
    );

    /**
     * 敏感信息泄露模式
     */
    private static final List<Pattern> SENSITIVE_INFO_PATTERNS = List.of(
            Pattern.compile("\\b\\d{17,19}\\b"),                    // 银行卡号
            Pattern.compile("\\b\\d{6}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]\\b"), // 身份证号
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"), // 邮箱
            Pattern.compile("\\b1[3-9]\\d{9}\\b"),                  // 手机号
            Pattern.compile("(?i)(password|passwd|pwd)\\s*[:=]\\s*\\S+"), // 密码
            Pattern.compile("(?i)(api[_-]?key|secret[_-]?key)\\s*[:=]\\s*\\S+")  // API Key
    );

    /**
     * 过滤用户输入，检测并中和 Prompt Injection 攻击
     * 
     * @param userInput 用户原始输入
     * @return 过滤结果（包含是否安全、风险描述、清理后的输入）
     */
    public FilterResult filter(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return FilterResult.safe(userInput);
        }

        List<String> risks = new java.util.ArrayList<>();
        String sanitized = userInput;

        // 1. 检测 Prompt Injection 模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                String matched = pattern.pattern();
                risks.add("PROMPT_INJECTION: " + matched);
                log.warn("[PromptSecurity] 检测到 Prompt Injection 尝试: pattern={}", matched);
                // 中和：将匹配内容替换为 [FILTERED]
                sanitized = pattern.matcher(sanitized).replaceAll("[FILTERED]");
            }
        }

        // 2. 检测敏感信息
        for (Pattern pattern : SENSITIVE_INFO_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                risks.add("SENSITIVE_INFO: " + pattern.pattern());
                log.warn("[PromptSecurity] 检测到敏感信息: pattern={}", pattern.pattern());
                sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
            }
        }

        // 3. 长度限制（防止 Token 炸弹）
        if (sanitized.length() > 10000) {
            risks.add("INPUT_TOO_LONG: " + sanitized.length() + " chars (max 10000)");
            sanitized = sanitized.substring(0, 10000) + "\n...[内容已截断]";
            log.warn("[PromptSecurity] 用户输入过长: {} 字符", userInput.length());
        }

        // 4. 检测异常字符比例（大量特殊字符可能是编码攻击）
        long specialCharCount = sanitized.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();
        double specialRatio = (double) specialCharCount / sanitized.length();
        if (specialRatio > 0.5 && sanitized.length() > 100) {
            risks.add("HIGH_SPECIAL_CHAR_RATIO: " + String.format("%.1f%%", specialRatio * 100));
            log.warn("[PromptSecurity] 异常字符比例: {}%", String.format("%.1f", specialRatio * 100));
        }

        if (risks.isEmpty()) {
            return FilterResult.safe(sanitized);
        } else {
            return FilterResult.blocked(sanitized, risks);
        }
    }

    /**
     * 快速检测（仅判断是否安全，不修改内容）
     */
    public boolean isSafe(String userInput) {
        if (userInput == null || userInput.isBlank()) return true;
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(userInput).find()) return false;
        }
        return true;
    }

    /**
     * 过滤结果
     */
    public static class FilterResult {
        private final boolean safe;
        private final String sanitizedInput;
        private final List<String> risks;

        private FilterResult(boolean safe, String sanitizedInput, List<String> risks) {
            this.safe = safe;
            this.sanitizedInput = sanitizedInput;
            this.risks = risks;
        }

        public static FilterResult safe(String input) {
            return new FilterResult(true, input, List.of());
        }

        public static FilterResult blocked(String sanitized, List<String> risks) {
            return new FilterResult(false, sanitized, risks);
        }

        public boolean isSafe() { return safe; }
        public String getSanitizedInput() { return sanitizedInput; }
        public List<String> getRisks() { return risks; }
        public boolean hasRisks() { return !risks.isEmpty(); }
    }
}
