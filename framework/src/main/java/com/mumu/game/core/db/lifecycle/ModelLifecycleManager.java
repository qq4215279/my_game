package com.mumu.game.core.db.lifecycle;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.meta.ModelRegistry;
import com.mumu.game.core.db.pool.ShardExecutorRouter;
import com.mumu.game.core.log.LogTopic;

/**
 * ModelLifecycleManager
 * 模型生命周期管理（预加载 / 下线 flush / 关服 flush）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelLifecycleManager {

    @Resource
    private ShardExecutorRouter shardExecutorRouter;

    /**
     * 异步预加载 routeId 相关模型
     */
    public void asyncPreload(long routeId) {
        shardExecutorRouter.execute(routeId, () -> preload(routeId));
    }

    /**
     * 预加载 routeId 相关模型
     */
    public void preload(long routeId) {
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.isPreLoad()) {
                continue;
            }
            try {
                BaseModel<?> model = ModelRegistry.getModelBean(meta.getEntityClass());
                model.preload(routeId);
            } catch (Exception e) {
                LogTopic.MODEL.error(e, "preload", "table", meta.getTableName(), "routeId", routeId);
            }
        }
    }

    /**
     * 异步 flush 并清理 routeId 缓存
     */
    public void asyncFlushAndClear(long routeId) {
        shardExecutorRouter.execute(routeId, () -> flushAndClear(routeId));
    }

    /**
     * 同步 flush 并清理 routeId 缓存（下线时使用）
     */
    public void flushAndClear(long routeId) {
        shardExecutorRouter.executeSync(routeId, () -> doFlushAndClear(routeId));
    }

    private void doFlushAndClear(long routeId) {
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            try {
                BaseModel<?> model = ModelRegistry.getModelBean(meta.getEntityClass());
                model.flushRoute(routeId);
                model.clearRouteCache(routeId);
            } catch (Exception e) {
                LogTopic.MODEL.error(e, "flushAndClear", "table", meta.getTableName(), "routeId", routeId);
            }
        }
    }

    /**
     * 关服时 flush 全部脏数据
     */
    public void flushAllOnShutdown() {
        LogTopic.MODEL.info("flushAllOnShutdown", "start");
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.hasDb() && !meta.hasRedis()) {
                continue;
            }
            try {
                BaseModel<?> model = ModelRegistry.getModelBean(meta.getEntityClass());
                model.flushAllDirty();
            } catch (Exception e) {
                LogTopic.MODEL.error(e, "flushAllOnShutdown", "table", meta.getTableName());
            }
        }
        LogTopic.MODEL.info("flushAllOnShutdown", "end");
    }
}
