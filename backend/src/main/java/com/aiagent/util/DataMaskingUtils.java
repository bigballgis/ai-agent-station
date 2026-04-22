package com.aiagent.util;

public final class DataMaskingUtils {

    private DataMaskingUtils() {}

    /**
     * 手机号脱敏: 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏: u***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        String prefix = atIndex > 1 ? email.substring(0, 1) + "***" : "***";
        return prefix + email.substring(atIndex);
    }

    /**
     * API Key 脱敏: sk-****abcd
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, Math.min(3, apiKey.length() - 4)) + "****" +
               apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 身份证号脱敏: 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 通用脱敏: 保留前 n 位和后 m 位
     */
    public static String mask(String value, int prefixLen, int suffixLen) {
        if (value == null) return null;
        int len = value.length();
        if (len <= prefixLen + suffixLen) return value;
        return value.substring(0, prefixLen) + "****" + value.substring(len - suffixLen);
    }
}
