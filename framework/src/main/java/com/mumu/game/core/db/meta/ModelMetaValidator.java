package com.mumu.game.core.db.meta;

import java.util.List;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.util.ReflectFieldUtil;

/**
 * ModelMetaValidator
 * 模型元数据启动校验器
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
final class ModelMetaValidator {

    private ModelMetaValidator() {
    }

    static void validate(Class<?> domainClass, List<IndexMeta> indexes) {
        if (!BaseEntity.class.isAssignableFrom(domainClass)) {
            throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 必须继承 BaseEntity");
        }
        if (indexes == null || indexes.isEmpty()) {
            throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 未定义 @ModelTable.indexes");
        }
        IndexMeta primary = indexes.get(0);
        if (!primary.isUnique()) {
            throw new IllegalStateException(
                "实体 " + domainClass.getSimpleName() + " 第一个索引必须是唯一索引: " + primary.getName());
        }
        if (!primary.isPrimary()) {
            throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 第一个索引必须标记为主索引");
        }
        for (IndexMeta index : indexes) {
            for (String field : index.getFields()) {
                if (!ReflectFieldUtil.hasField(domainClass, field)) {
                    throw new IllegalStateException(
                        "实体 " + domainClass.getSimpleName() + " 索引字段不存在: " + field);
                }
            }
        }
    }
}
