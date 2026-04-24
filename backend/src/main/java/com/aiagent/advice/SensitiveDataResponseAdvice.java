package com.aiagent.advice;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import com.aiagent.util.DataMaskingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * 敏感数据响应体增强器
 *
 * 在 API 响应发送到客户端之前，对标注了 @Sensitive 注解的字段进行脱敏处理。
 * 作为 Jackson @Sensitive 序列化器的补充保障层，确保即使序列化器配置遗漏，
 * 敏感数据也不会以明文形式返回给客户端。
 *
 * 注意：此增强器主要作为防御性编程的补充措施。
 * 主要的脱敏逻辑由 SensitiveDataSerializer（Jackson）处理。
 */
@ControllerAdvice(basePackages = "com.aiagent.controller")
public class SensitiveDataResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataResponseAdvice.class);

    private final ObjectMapper objectMapper;

    public SensitiveDataResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        try {
            maskSensitiveFields(body);
        } catch (Exception e) {
            log.warn("敏感数据脱敏处理失败: {}", e.getMessage());
        }
        return body;
    }

    /**
     * 递归处理对象中的敏感字段
     */
    private void maskSensitiveFields(Object obj) {
        if (obj == null) return;

        // 处理集合类型
        if (obj instanceof Collection<?>) {
            for (Object item : (Collection<?>) obj) {
                maskSensitiveFields(item);
            }
            return;
        }

        // 处理 Map 类型
        if (obj instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (entry.getValue() != null) {
                    maskSensitiveFields(entry.getValue());
                }
            }
            return;
        }

        // 处理普通对象
        Class<?> clazz = obj.getClass();
        // 跳过 JDK 基础类型和常见框架类型
        if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("com.fasterxml.")
                || clazz.getName().startsWith("org.springframework.")) {
            return;
        }

        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (value instanceof String && !((String) value).isEmpty()) {
                        String masked = maskValue((String) value, sensitive);
                        field.set(obj, masked);
                    }
                } catch (IllegalAccessException e) {
                    log.debug("无法访问敏感字段: {}.{}", clazz.getSimpleName(), field.getName());
                }
            }
        }
    }

    /**
     * 根据注解配置对值进行脱敏
     */
    private String maskValue(String value, Sensitive sensitive) {
        SensitiveType type = sensitive.type();
        switch (type) {
            case PASSWORD:
                return "******";
            case PARTIAL:
                return DataMaskingUtils.mask(value, sensitive.maskPrefix(), sensitive.maskSuffix());
            case EMAIL:
                return DataMaskingUtils.maskEmail(value);
            case PHONE:
                return DataMaskingUtils.maskPhone(value);
            default:
                return "******";
        }
    }

    /**
     * 获取类及其父类的所有字段
     */
    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}
