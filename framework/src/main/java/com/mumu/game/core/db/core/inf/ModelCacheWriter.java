package com.mumu.game.core.db.core.inf;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;

import java.util.List;

/**
 * ModelCacheWriter
 * 缓存写接口（JVM / Redis 统一写 API）
 * @author liuzhen
 * @version 1.0.0 2026/7/10
 */
public interface ModelCacheWriter {

    /**
     * 保存单条记录到缓存
     * @param meta   表元数据
     * @param entity 实体对象
     */
    void save(ModelMeta meta, BaseEntity entity);

    /**
     * 批量保存
     * @param meta   表元数据
     * @param entities 实体对象列表
     * @since 2026/7/13 17:06
     */
    void saveBatch(ModelMeta meta, List<? extends BaseEntity> entities);

    /**
     * 按完整索引键删除单条
     * @param primaryRouteId 主索引路由id
     * @param index 索引元数据（可通过 {@code ModelRegistry.getMeta(index.getEntityClass())} 取表元数据）
     * @param keys  完整索引键
     */
    void delete(long primaryRouteId, IndexMeta index, Object... keys);

    /**
     * 按索引左前缀批量删除
     * @param index 索引元数据
     * @param keys  索引键（支持左前缀）
     * @return 被删除的实体列表（Memory 返回实际删除项；Redis 通常返回空列表）
     */
    List<BaseEntity> deleteByPrefix(IndexMeta index, Object... keys);
}
