package com.aiagent.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Message utility for resolving i18n message codes.
 * Provides static access to Spring MessageSource for use in non-Spring-managed classes.
 */
@Component
public class MessageUtils {

    private static MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    /**
     * Get message by code using the current LocaleContextHolder locale.
     *
     * @param code message code
     * @param args optional format arguments
     * @return resolved message
     */
    public static String getMessage(String code, Object... args) {
        return getMessage(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * Get message by code using the specified locale.
     *
     * @param code   message code
     * @param locale locale to use
     * @param args   optional format arguments
     * @return resolved message
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        if (messageSource == null) {
            return code;
        }
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            return code;
        }
    }
}
