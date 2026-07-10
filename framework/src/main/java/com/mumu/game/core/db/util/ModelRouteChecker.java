package com.mumu.game.core.db.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.config.DbPersistProperties;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.thread.ThreadPoolRouter;

/**
 * ModelRouteChecker
 * 写操作路由线程校验
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelRouteChecker {

    @Autowired
    private DbPersistProperties persistProperties;

    /**
     * 校验写操作是否在 routeId 对应业务线程执行
     */
    public void checkWrite(long routeId, ModelMeta meta) {
        if (!persistProperties.isThreadCheckEnabled() || meta.isSkipThreadCheck()) {
            return;
        }
        if (ThreadPoolRouter.isPlayerThread(routeId)) {
            return;
        }
        String message = "非法缓存写操作，routeId=" + routeId + ", table=" + meta.getTableName()
            + ", thread=" + Thread.currentThread().getName();
        if (persistProperties.isStrictThreadCheck()) {
            throw new IllegalStateException(message);
        }
        LogTopic.MODEL.error("illegalCacheWrite", "routeId", routeId, "table", meta.getTableName(), "thread",
            Thread.currentThread().getName());
    }
}
