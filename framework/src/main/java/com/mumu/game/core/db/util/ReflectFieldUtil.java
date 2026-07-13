package com.mumu.game.core.db.util;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

/**
 * ReflectFieldUtil
 * 实体字段反射工具
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public final class ReflectFieldUtil {

    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private ReflectFieldUtil() {
    }

    /**
     * 读取实体字段值
     */
    public static Object getFieldValue(Object entity, String fieldName) {
        Field field = resolveField(entity.getClass(), fieldName);
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取字段失败: " + fieldName, e);
        }
    }

    /**
     * 读取 long 类型字段
     */
    public static long getLongValue(Object entity, String fieldName) {
        Object value = getFieldValue(entity, fieldName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("字段不是数值类型: " + fieldName);
    }

    /**
     * 设置实体字段值
     */
    public static void setFieldValue(Object entity, String fieldName, Object value) {
        Field field = resolveField(entity.getClass(), fieldName);
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("设置字段失败: " + fieldName, e);
        }
    }

    /**
     * 解析并缓存实体字段（供索引访问器构建使用）
     */
    public static Field resolveField(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#" + fieldName;
        return FIELD_CACHE.computeIfAbsent(cacheKey, k -> {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    Field field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new IllegalArgumentException("实体 " + clazz.getSimpleName() + " 不存在字段: " + fieldName);
        });
    }

    /**
     * 校验字段是否存在
     */
    public static boolean hasField(Class<?> clazz, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return false;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                current.getDeclaredField(fieldName);
                return true;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return false;
    }
}
