package com.mumu.game.core.db.lifecycle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.mumu.game.core.thread.ThreadPoolRouter;
import com.mumu.game.core.thread2.GameEventExecutorGroup;

/**
 * ShardExecutorRouter
 * 分片任务路由（主索引第一个字段作为 routeId）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ShardExecutorRouter {

    private static final long SYNC_TIMEOUT_SECONDS = 30L;

    @Resource
    private GameEventExecutorGroup modelExecutor;

    /**
     * 提交到 routeId 对应业务线程
     */
    public void execute(long routeId, Runnable task) {
        if (modelExecutor != null) {
            modelExecutor.execute(routeId, task);
            return;
        }
        task.run();
    }

    /**
     * 同步在 routeId 对应业务线程执行任务
     */
    public void executeSync(long routeId, Runnable task) {
        if (modelExecutor == null || ThreadPoolRouter.isPlayerThread(routeId)) {
            task.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        modelExecutor.execute(routeId, () -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("routeId 同步任务超时: " + routeId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("routeId 同步任务被中断: " + routeId, e);
        }
        if (error.get() != null) {
            if (error.get() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("routeId 同步任务失败: " + routeId, error.get());
        }
    }
}
