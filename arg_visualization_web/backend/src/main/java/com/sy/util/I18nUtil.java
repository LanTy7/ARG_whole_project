package com.sy.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 */
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 获取国际化消息
     * @param code 消息代码
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }

    /**
     * 获取国际化消息（带参数）
     * @param code 消息代码
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 获取国际化消息（指定语言）
     * @param code 消息代码
     * @param locale 语言区域
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }

    /**
     * 获取国际化消息（带参数，指定语言）
     * @param code 消息代码
     * @param args 参数
     * @param locale 语言区域
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
