package com.mumu.game.core.net.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mumu.game.constants.ThreadConstants;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.properties.ThreadPoolProperties;
import com.mumu.game.core.thread.ThreadPoolRouter;
import com.mumu.game.core.thread2.GameEventExecutorGroup;
import com.mumu.game.core.utils.ThreadPoolUtil;

import jakarta.annotation.PreDestroy;

/**
 * ThreadPoolConfiguration
 * 线程池配置类
 * @author liuzhen
 * @version 1.0.0 2026/5/4 17:14
 */
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
// 这个注解组合起来表示：只有当配置文件（如 application.yml）中存在 net.server.enable=true 时，才会加载被注解的 Bean 或配置类。
@ConditionalOnProperty(prefix = "net.thread", name = "enable", havingValue = "true")
public class ThreadPoolConfiguration {

    private GameEventExecutorGroup playerExecutor;
    private ThreadPoolExecutor serverExecutor;
    private ScheduledExecutorService scheduledExecutor;
    private ThreadPoolRouter threadPoolRouter;

    private final ThreadPoolProperties properties;

    public ThreadPoolConfiguration(ThreadPoolProperties properties) {
        this.properties = properties;
    }

    /** 玩家线程池（根据玩家id路由到对应线程处理，且每个线程持有一个任务队列） */
    @Bean
    public GameEventExecutorGroup playerExecutor() {
        LogTopic.NET.info("playerExecutor", "corePoolSize", properties.getPlayerCorePoolSize(), "maxQueueSize",
            properties.getPlayerMaxQueueSize());
        return playerExecutor = new GameEventExecutorGroup(ThreadConstants.THREAD_PREFIX_PLAYER,
            properties.getPlayerCorePoolSize(), properties.getPlayerMaxQueueSize());
    }

    /** 服务内部线程池（请求随机分配给空闲线程处理） */
    @Bean
    public ThreadPoolExecutor serverExecutor() {
        LogTopic.NET.info("serverExecutor", "corePoolSize", properties.getServerCorePoolSize(), "maxPoolSize",
            properties.getServerMaxPoolSize(), "keepAliveTime", properties.getServerKeepAliveTime(), "maxQueueSize",
            properties.getServerMaxQueueSize());
        return serverExecutor = ThreadPoolUtil.newExecutor(ThreadConstants.THREAD_PREFIX_SERVER,
            properties.getServerCorePoolSize(), properties.getServerMaxPoolSize(), properties.getServerKeepAliveTime(),
            properties.getServerMaxQueueSize());
    }

    /** 服务定时线程池（处理定时任务） */
    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        LogTopic.NET.info("scheduledExecutor", "corePoolSize", properties.getServerCorePoolSize());
        return scheduledExecutor = ThreadPoolUtil.newScheduledExecutor(properties.getServerCorePoolSize(),
            ThreadConstants.THREAD_PREFIX_SCHEDULED);
    }

    /** 线程路由器 */
    @Bean
    public ThreadPoolRouter threadPoolRouter() {
        return this.threadPoolRouter = new ThreadPoolRouter(playerExecutor(), serverExecutor());
    }

    @PreDestroy
    public void destroy() {
        LogTopic.NET.info("容器关闭 ExecutorPool stop...");
        try {
            playerExecutor.shutdown();
            serverExecutor.shutdown();
            scheduledExecutor.shutdown();
            LogTopic.NET.info("容器关闭 ExecutorPool end");
        } catch (Exception e) {
            LogTopic.NET.error(e, "容器关闭 ExecutorPool destroy error");
        }
    }

    /* @AutoScheduled(key = AutoScheduleKey.LOG_THREAD_INFO, cron = Cron.EVERY_5_MINUTE)
    public void logThreadInfo() {
        if (!ConfigSwitchEnum.LOG_SYSTEM.getBool())
            return;
        if (playerExecutor != null) {
            for (int i = 0; i < playerExecutor.executors.length; i++) {
                logInfo("threadInfo-playerExecutor-" + i, playerExecutor.executors[i]);
            }
        }
        if (serverExecutor != null) {
            logInfo("threadInfo-serverExecutor", serverExecutor);
        }
        if (threadPoolRouter != null) {
            LogTopic.ACTION.info("threadInfo-threadPoolRouter",
                // 任务对象池可用大小
                "remainingWorkPool", threadPoolRouter.workPool.remainingCapacity(),
                // 玩家任务数量
                "playerTaskCount", threadPoolRouter.playerTaskCount.longValue(),
                // 内部服务任务数量
                "serverTaskCount", threadPoolRouter.serverTaskCount.longValue(),
                // 完成的任务数量
                "processTaskCount", threadPoolRouter.processTaskCount.longValue());
        }
    } */

    private void logInfo(String action, ThreadPoolExecutor pool) {
        LogTopic.ACTION.info(action,
            // 池中线程的数量
            "poolSize", pool.getPoolSize(),
            // 核心线程的数量
            "corePoolSize", pool.getCorePoolSize(),
            // 最大线程数
            "maximumPoolSize", pool.getMaximumPoolSize(),
            // 正在执行任务的线程数量
            "activeCount", pool.getActiveCount(),
            // 已完成任务的数量
            "completedTaskCount", pool.getCompletedTaskCount(),
            // 任务总数
            "taskCount", pool.getTaskCount(),
            // 是否已关闭
            "isShutdown", pool.isShutdown(),
            // 是否正在终止
            "isTerminating", pool.isTerminating(),
            // 是否已终止
            "isTerminated", pool.isTerminated());
    }
}
