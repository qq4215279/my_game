package com.mumu.game.core.db.core;

import com.mumu.game.core.db.core.meta.ModelMeta;

/**
 * ModelHook
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/10 15:24
 */
public interface ModelHook<Entity extends BaseEntity> {
    /**
     * 启动时绑定元数据
     * @param modelMeta 表模型运行时元数据
     * @since 2026/7/10 15:25
     */
    void bindMeta(ModelMeta modelMeta);

    /**
     * flush 指定 routeId 的全部脏数据
     */
    void flushRoute(long routeId);

    /**
     * 清理指定 routeId 的 JVM 缓存
     */
    void clearRouteCache(long routeId);

    /**
     * flush 当前表全部脏数据（关服使用）
     */
    void flushAllDirty();

    /**
     * 预加载分片数据（L2 Redis → L1 JVM；Redis 无数据时从 DB 加载并回填）
     */
    void preload(long routeId);

    /**
     * 定时器重试调用
     */
    void retryFlush(String cacheKey);
}

