package com.mumu.game.core.db.cache.inf;

import java.util.List;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;

/**
 * ModelCacheReader
 * 缓存读接口（JVM / Redis 统一读 API）
 * @author liuzhen
 * @version 1.0.0 2026/7/10
 */
public interface ModelCacheReader {

    /**
     * 按完整索引键查询单条
     * @param meta  表元数据
     * @param index 索引元数据
     * @param clazz 实体类型
     * @param keys  完整索引键
     * @return 实体，不存在返回 null
     */
    <Entity extends BaseEntity> Entity getOne(ModelMeta meta, IndexMeta index, Class<Entity> clazz, Object... keys);

    /**
     * 按索引左前缀查询列表
     * @param meta  表元数据
     * @param index 索引元数据
     * @param clazz 实体类型
     * @param keys  索引键（支持左前缀）
     * @return 匹配列表，无数据返回空列表
     */
    <Entity extends BaseEntity> List<Entity> getList(ModelMeta meta, IndexMeta index, Class<Entity> clazz, Object... keys);
}
