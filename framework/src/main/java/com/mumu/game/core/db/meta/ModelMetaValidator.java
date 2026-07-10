package com.mumu.game.core.db.meta;

import java.util.List;

import com.mumu.game.core.db.util.ReflectFieldUtil;

/**
 * ModelMetaValidator
 * 模型元数据启动校验器
 * <p>在 {@link ModelMeta} 构建时执行，不通过则启动失败</p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
final class ModelMetaValidator {

    private ModelMetaValidator() {
    }

    /**
     * 校验索引配置合法性
     *
     * @param domainClass 实体类
     * @param indexes     解析后的索引列表
     */
    static void validate(Class<?> domainClass, List<IndexMeta> indexes) {
        // 规则1：必须定义至少一个索引
        if (indexes == null || indexes.isEmpty()) {
            throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 未定义 @ModelTable.indexes");
        }
        // 规则2：第一个索引必须是唯一索引（并发边界 + 默认查询索引）
        IndexMeta primary = indexes.get(0);
        if (!primary.isUnique()) {
            throw new IllegalStateException(
                "实体 " + domainClass.getSimpleName() + " 第一个索引必须是唯一索引: " + primary.getName());
        }
        // 规则3：索引字段必须在实体类中存在
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
