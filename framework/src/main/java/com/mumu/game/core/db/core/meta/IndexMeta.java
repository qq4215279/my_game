package com.mumu.game.core.db.core.meta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.mumu.game.core.db.anno.Index;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.util.ReflectFieldUtil;

import lombok.Getter;

/**
 * IndexMeta
 * 索引运行时元数据，由 {@link Index} 注解在启动期解析生成。
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Getter
public final class IndexMeta {

    /** 索引名称 */
    private final String name;
    /** 索引列（实体字段名） */
    private final String[] fields;
    /** 是否唯一索引 */
    private final boolean unique;
    /** 是否主索引 */
    private final boolean primary;
    /** 索引字段访问器（MethodHandle，启动期构建） */
    private final IndexFieldAccessor accessor;

    private IndexMeta(String name, String[] fields, boolean unique, boolean primary, IndexFieldAccessor accessor) {
        this.name = name;
        this.fields = fields;
        this.unique = unique;
        this.primary = primary;
        this.accessor = accessor;
    }

    /**
     * 从 {@link Index} 注解构建索引元数据
     *
     * @param entityClass 实体类
     * @param index       索引注解
     * @param primary     是否主索引
     */
    public static IndexMeta from(Class<?> entityClass, Index index, boolean primary) {
        String[] fields = index.value();
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("索引字段不能为空");
        }
        String name = index.name();
        if (StringUtils.isBlank(name)) {
            String prefix = index.unique() ? "unq_" : "idx_";
            name = prefix + String.join("_", fields);
        }
        IndexFieldAccessor accessor = IndexFieldAccessor.create(entityClass, fields);
        return new IndexMeta(name, fields.clone(), index.unique(), primary, accessor);
    }

    public boolean isFullKey(Object... keys) {
        return keys != null && keys.length == fields.length;
    }

    /**
     * 左前缀匹配：实体字段值与 keys 逐位相等
     */
    public boolean matchPrefix(BaseEntity entity, Object... keys) {
        if (keys == null || keys.length > fields.length) {
            return false;
        }
        if (primary) {
            return matchPrimaryPrefix(entity, keys);
        }
        return accessor.matchPrefix(entity, keys);
    }

    private boolean matchPrimaryPrefix(BaseEntity entity, Object... keys) {
        for (int i = 0; i < keys.length; i++) {
            Object fieldValue = i == 0 ? entity.getPrimaryRouteId() : accessor.readAt(entity, i);
            if (!Objects.equals(String.valueOf(fieldValue), String.valueOf(keys[i]))) {
                return false;
            }
        }
        return true;
    }

    public String buildKeyString(Object... keys) {
        if (keys == null || keys.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(keys[i]);
        }
        return sb.toString();
    }

    /**
     * 构建副索引键（末尾带冒号，避免左前缀 startsWith 误匹配，如 10: 与 100:）
     * <p>完整键示例：{@code 10001:3:}；左前缀示例：{@code 10001:}</p>
     */
    public String buildIndexKey(Object... keys) {
        if (keys == null || keys.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (Object key : keys) {
            sb.append(key).append(':');
        }
        return sb.toString();
    }

    /** 从实体构建副索引键 */
    public String buildIndexKey(BaseEntity entity) {
        return buildIndexKey(readKeyValues(entity));
    }

    /**
     * 从实体读取本索引全部字段值
     */
    public Object[] readKeyValues(BaseEntity entity) {
        if (primary) {
            return readPrimaryKeyValues(entity);
        }
        return accessor.readAll(entity);
    }

    private Object[] readPrimaryKeyValues(BaseEntity entity) {
        if (fields.length == 1) {
            return new Object[] {entity.getPrimaryRouteId()};
        }
        Object[] values = new Object[fields.length];
        values[0] = entity.getPrimaryRouteId();
        for (int i = 1; i < fields.length; i++) {
            values[i] = accessor.readAt(entity, i);
        }
        return values;
    }

    /**
     * 索引字段访问器（MethodHandle，启动期构建）
     */
    static final class IndexFieldAccessor {
        /**  */
        private final MethodHandle[] readers;

        private IndexFieldAccessor(MethodHandle[] readers) {
            this.readers = readers;
        }

        static IndexFieldAccessor create(Class<?> entityClass, String[] fieldNames) {
            MethodHandle[] readers = new MethodHandle[fieldNames.length];
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            for (int i = 0; i < fieldNames.length; i++) {
                readers[i] = resolveReader(lookup, entityClass, fieldNames[i]);
            }
            return new IndexFieldAccessor(readers);
        }

        private static MethodHandle resolveReader(MethodHandles.Lookup lookup, Class<?> clazz, String fieldName) {
            String capitalized = capitalize(fieldName);
            try {
                Method getter = clazz.getMethod("get" + capitalized);
                return lookup.unreflect(getter);
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
                // boolean 字段可能为 isXxx
            }
            try {
                Method getter = clazz.getMethod("is" + capitalized);
                return lookup.unreflect(getter);
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
                // 回退 Field
            }
            try {
                Field field = ReflectFieldUtil.resolveField(clazz, fieldName);
                return lookup.unreflectGetter(field);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("无法构建索引访问器: " + clazz.getSimpleName() + "#" + fieldName, e);
            }
        }


        Object readAt(Object entity, int index) {
            try {
                return readers[index].invoke(entity);
            } catch (Throwable e) {
                throw new IllegalStateException("读取索引字段失败, index=" + index, e);
            }
        }

        Object[] readAll(Object entity) {
            Object[] values = new Object[readers.length];
            for (int i = 0; i < readers.length; i++) {
                values[i] = readAt(entity, i);
            }
            return values;
        }

        boolean matchPrefix(Object entity, Object... keys) {
            for (int i = 0; i < keys.length; i++) {
                Object fieldValue = readAt(entity, i);
                if (!Objects.equals(String.valueOf(fieldValue), String.valueOf(keys[i]))) {
                    return false;
                }
            }
            return true;
        }

        private static String capitalize(String name) {
            if (name == null || name.isEmpty()) {
                return name;
            }
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
