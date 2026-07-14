package com.mumu.game.core.db.util;

import org.springframework.stereotype.Component;

import com.mumu.game.core.db.config.DbPersistProperties;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.thread.ThreadPoolRouter;

import jakarta.annotation.Resource;

/**
 * ModelRouteChecker
 * 写操作路由线程校验
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelRouteChecker {

    @Resource
    private DbPersistProperties persistProperties;

    /**
     * 校验写操作是否在 routeId 对应业务线程执行
     * @return {@code true} 允许写；{@code false} 拒绝写（方法内已打日志）
     */
    public boolean checkWrite(long routeId, ModelMeta meta) {
        if (!persistProperties.isThreadCheckEnabled() || meta.isSkipThreadCheck()) {
            return true;
        }
        if (ThreadPoolRouter.isPlayerThread(routeId)) {
            return true;
        }
        LogTopic.MODEL.error("illegalCacheWrite", "routeId", routeId, "table", meta.getTableName(), "thread",
            Thread.currentThread().getName());
        return false;
    }
}
