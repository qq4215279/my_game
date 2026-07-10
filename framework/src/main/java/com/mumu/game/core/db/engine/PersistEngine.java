package com.mumu.game.core.db.engine;

import java.util.Collection;
import java.util.List;

import com.mumu.game.core.db.cache.inf.ModelCacheReader;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;

/**
 * PersistEngine
 * 持久化引擎接口（L3 数据库层）
 * <p>
 * 读：供 select 懒加载穿透 DB、preload 预加载使用；
 * 写：供异步 flush、persistNow 同步落库使用。
 * </p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public interface PersistEngine {

    /** 引擎类型标识，如 mongo / mysql */
    String type();

    /**
     * 按索引完整键查询单条
     * <p>用于 select 懒加载穿透 DB、preload 预加载</p>
     */
    <Domain extends BaseEntity> Domain findOne(ModelMeta meta, IndexMeta index, Class<Domain> clazz, Object... keys);

    /**
     * 按索引左前缀查询列表
     * <p>用于 select 懒加载穿透 DB、preload 预加载</p>
     */
    <T extends BaseEntity> List<T> findList(ModelMeta meta, IndexMeta index, Class<T> clazz, Object... keys);

    /** 新增或更新单条 */
    void upsert(ModelMeta meta, BaseEntity entity);

    /** 批量新增或更新 */
    void upsertBatch(ModelMeta meta, Collection<? extends BaseEntity> entities);

    /** 按完整索引键删除 */
    void delete(ModelMeta meta, IndexMeta index, Object... keys);
}
