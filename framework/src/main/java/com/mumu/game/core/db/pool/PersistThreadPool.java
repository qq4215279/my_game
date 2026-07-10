package com.mumu.game.core.db.pool;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * PersistThreadPool
 * 持久化任务池（按表划分 pending，按 routeId 路由到业务线程执行）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class PersistThreadPool {

    /** tableName -> 待执行 cacheKey 集合 */
    private final Map<String, Set<String>> pendingByTable = new ConcurrentHashMap<>();

    @Resource
    private ShardExecutorRouter shardExecutorRouter;

    /**
     * 提交落库任务（同 cacheKey 仅调度一次）
     */
    public void submit(long routeId, String tableName, String cacheKey, Runnable flushTask) {
        shardExecutorRouter.execute(routeId, () -> runOnce(tableName, cacheKey, flushTask));
    }

    private void runOnce(String tableName, String cacheKey, Runnable flushTask) {
        Set<String> pending = pendingByTable.computeIfAbsent(tableName, k -> new HashSet<>());
        synchronized (pending) {
            if (!pending.add(cacheKey)) {
                return;
            }
        }
        try {
            flushTask.run();
        } finally {
            synchronized (pending) {
                pending.remove(cacheKey);
            }
        }
    }
}
