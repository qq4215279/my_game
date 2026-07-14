package com.mumu.game.core.db.lifecycle;

import com.mumu.game.core.db.bootstrap.ModelRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;

/**
 * ModelLifecycleManager
 * 模型生命周期管理（预加载 / 下线 flush / 关服 flush）
 * <p>仅处理玩家维度表（{@code skipThreadCheck=false}），非玩家表由业务自行管理</p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelLifecycleManager {

    @Resource
    private ShardExecutorRouter shardExecutorRouter;

    /**
     * 异步预加载玩家 routeId 相关模型
     */
    public void asyncPreload(long routeId) {
        shardExecutorRouter.execute(routeId, () -> preload(routeId));
    }

    /**
     * 预加载玩家 routeId 相关模型（仅玩家维度表）
     */
    public void preload(long routeId) {
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.isPlayerScoped() || !meta.isPreLoad()) {
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
     * 异步 flush 并清理玩家 routeId 缓存
     */
    public void asyncFlushAndClear(long routeId) {
        shardExecutorRouter.execute(routeId, () -> flushAndClear(routeId));
    }

    /**
     * 同步 flush 并清理玩家 routeId 缓存（下线时使用，仅玩家维度表）
     */
    public void flushAndClear(long routeId) {
        shardExecutorRouter.executeSync(routeId, () -> doFlushAndClear(routeId));
    }

    private void doFlushAndClear(long routeId) {
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.isPlayerScoped()) {
                continue;
            }
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
