package com.aiagent.security.validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    /**
     * 验证密码是否符合安全策略
     *
     * @param password 待验证的密码
     * @return 错误信息列表，如果为空则表示密码合规
     */
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("密码长度不能少于" + MIN_LENGTH + "个字符");
        }
        if (password != null && password.length() > MAX_LENGTH) {
            errors.add("密码长度不能超过" + MAX_LENGTH + "个字符");
        }
        if (password != null && !UPPER.matcher(password).find()) {
            errors.add("密码必须包含至少一个大写字母");
        }
        if (password != null && !LOWER.matcher(password).find()) {
            errors.add("密码必须包含至少一个小写字母");
        }
        if (password != null && !DIGIT.matcher(password).find()) {
            errors.add("密码必须包含至少一个数字");
        }
        if (password != null && !SPECIAL.matcher(password).find()) {
            errors.add("密码必须包含至少一个特殊字符");
        }
        // 检查常见弱密码
        if (password != null && isCommonPassword(password)) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }
        return errors;
    }

    /**
     * 快速校验密码是否合规
     *
     * @param password 待验证的密码
     * @return true表示合规
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    private boolean isCommonPassword(String password) {
        String[] common = {
                "123456", "password", "12345678", "qwerty", "abc123",
                "admin123", "letmein", "welcome", "monkey", "dragon",
                "master", "login", "princess", "football", "shadow"
        };
        String lower = password.toLowerCase();
        for (String c : common) {
            if (c.equals(lower)) return true;
        }
        return false;
    }
}
