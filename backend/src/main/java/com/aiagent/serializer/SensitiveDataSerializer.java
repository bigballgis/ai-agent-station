package com.aiagent.serializer;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import com.aiagent.util.DataMaskingUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 敏感数据 Jackson 序列化器
 *
 * 在 JSON 序列化时自动对标注了 @Sensitive 的字段进行脱敏处理。
 * 支持通过 @Sensitive 注解的 type 属性指定不同的脱敏策略。
 *
 * 使用方式：在字段上标注 @Sensitive 注解，Jackson 会自动使用此序列化器。
 *
 * 脱敏策略：
 * - PASSWORD: 完全掩码为 "******"
 * - PARTIAL: 保留前 maskPrefix 位和后 maskSuffix 位
 * - EMAIL: 邮箱格式掩码
 * - PHONE: 手机号格式掩码
 */
public class SensitiveDataSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type = SensitiveType.PASSWORD;
    private int maskPrefix = 4;
    private int maskSuffix = 4;

    public SensitiveDataSerializer() {
    }

    public SensitiveDataSerializer(SensitiveType type, int maskPrefix, int maskSuffix) {
        this.type = type;
        this.maskPrefix = maskPrefix;
        this.maskSuffix = maskSuffix;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(mask(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Sensitive sensitive = property.getAnnotation(Sensitive.class);
            if (sensitive != null) {
                return new SensitiveDataSerializer(sensitive.type(), sensitive.maskPrefix(), sensitive.maskSuffix());
            }
        }
        return this;
    }

    /**
     * 根据脱敏类型对值进行脱敏
     */
    private String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (type) {
            case PASSWORD:
                return "******";
            case PARTIAL:
                return DataMaskingUtils.mask(value, maskPrefix, maskSuffix);
            case EMAIL:
                return DataMaskingUtils.maskEmail(value);
            case PHONE:
                return DataMaskingUtils.maskPhone(value);
            default:
                return "******";
        }
    }
}
