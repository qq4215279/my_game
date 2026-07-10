package com.mumu.game.core.db.meta;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.mumu.game.core.db.anno.Index;

import lombok.Data;

/**
 * IndexMeta
 * 索引运行时元数据，由 {@link Index} 注解在启动期解析生成。
 * <p>
 * 用于：
 * <ul>
 *   <li>判断查询键是否完整（selectOne）或左前缀（selectList）</li>
 *   <li>构建 cacheKey、Redis hashField</li>
 *   <li>从实体读取索引字段值</li>
 * </ul>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Data
public final class IndexMeta {

    /** 索引名称，如 unq_playerid / idx_playerid_functionid */
    private final String name;
    /** 索引列（实体字段名），顺序与 @Index.value 一致 */
    private final String[] fields;
    /** 是否唯一索引 */
    private final boolean unique;

    private IndexMeta(String name, String[] fields, boolean unique) {
        this.name = name;
        this.fields = fields;
        this.unique = unique;
    }

    /**
     * 从 {@link Index} 注解构建索引元数据
     * @param index 索引注解
     * @return IndexMeta
     */
    public static IndexMeta from(Index index) {
        String[] fields = index.value();
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("索引字段不能为空");
        }
        String name = index.name();
        // 未指定名称时自动生成：unq_字段 / idx_字段1_字段2
        if (StringUtils.isBlank(name)) {
            String prefix = index.unique() ? "unq_" : "idx_";
            name = prefix + String.join("_", fields);
        }
        return new IndexMeta(name, fields.clone(), index.unique());
    }

    /**
     * 是否为完整索引键（键数量与索引列数一致）
     * <p>完整键用于 selectOne / deleteOne</p>
     */
    public boolean isFullKey(Object... keys) {
        return keys != null && keys.length == fields.length;
    }

    /**
     * 左前缀匹配：实体字段值与 keys 逐位相等
     * <p>keys 数量小于 fields.length 时用于 selectList</p>
     */
    public boolean matchPrefix(Object entity, Object... keys) {
        if (keys == null || keys.length > fields.length) {
            return false;
        }
        for (int i = 0; i < keys.length; i++) {
            Object fieldValue = ModelFieldReader.read(entity, fields[i]);
            if (!Objects.equals(String.valueOf(fieldValue), String.valueOf(keys[i]))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将索引键拼接为字符串，如 10001 或 10001:3
     */
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
     * 从实体读取本索引全部字段值
     */
    public Object[] readKeyValues(Object entity) {
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = ModelFieldReader.read(entity, fields[i]);
        }
        return values;
    }
}
