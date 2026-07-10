package com.mumu.game.core.db.meta;

import com.mumu.game.core.db.util.ReflectFieldUtil;

/**
 * ModelFieldReader
 * 实体字段读取工具（包内可见，避免 IndexMeta 与 ReflectFieldUtil 循环依赖）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
final class ModelFieldReader {

    private ModelFieldReader() {
    }

    /**
     * 读取实体字段值
     */
    static Object read(Object entity, String fieldName) {
        return ReflectFieldUtil.getFieldValue(entity, fieldName);
    }

    /**
     * 读取 long 类型字段值
     */
    static long readLong(Object entity, String fieldName) {
        return ReflectFieldUtil.getLongValue(entity, fieldName);
    }
}
