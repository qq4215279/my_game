/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.thread.config;

import com.mumu.common.constants.ThreadConstants;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.thread.ThreadPoolGroupExecutor;
import com.mumu.framework.core.thread.ThreadPoolRouter;
import com.mumu.framework.core.util2.ThreadPoolUtil;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ThreadPoolConfiguration
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:52
 */
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
@ConditionalOnProperty(prefix = "net.thread", name = "enable", havingValue = "true")
public class ThreadPoolConfiguration {
  private ThreadPoolGroupExecutor playerExecutor;
  private ThreadPoolExecutor serverExecutor;
  private ScheduledExecutorService scheduledExecutor;

  private final ThreadPoolProperties properties;

  public ThreadPoolConfiguration(ThreadPoolProperties properties) {
    this.properties = properties;
  }

  /** 玩家线程池（根据玩家id路由到对应线程处理，且每个线程持有一个任务队列） */
  @Bean
  public ThreadPoolGroupExecutor playerExecutor() {
    LogTopic.NET.info(
        "playerExecutor",
        "corePoolSize",
        properties.getPlayerCorePoolSize(),
        "maxQueueSize",
        properties.getPlayerMaxQueueSize());
    return playerExecutor =
        new ThreadPoolGroupExecutor(
            properties.getPlayerCorePoolSize(), properties.getPlayerMaxQueueSize());
  }

  /** 服务内部线程池（请求随机分配给空闲线程处理） */
  @Bean
  public ThreadPoolExecutor serverExecutor() {
    LogTopic.NET.info(
        "serverExecutor",
        "corePoolSize",
        properties.getServerCorePoolSize(),
        "maxPoolSize",
        properties.getServerMaxPoolSize(),
        "keepAliveTime",
        properties.getServerKeepAliveTime(),
        "maxQueueSize",
        properties.getServerMaxQueueSize());
    return serverExecutor =
        ThreadPoolUtil.newExecutor(
            ThreadConstants.THREAD_PREFIX_SERVER,
            properties.getServerCorePoolSize(),
            properties.getServerMaxPoolSize(),
            properties.getServerKeepAliveTime(),
            properties.getServerMaxQueueSize());
  }

  /** 服务定时线程池（处理定时任务） */
  @Bean
  public ScheduledExecutorService scheduledExecutor() {
    LogTopic.NET.info("scheduledExecutor", "corePoolSize", properties.getServerCorePoolSize());
    return scheduledExecutor =
        ThreadPoolUtil.newScheduledExecutor(
            properties.getServerCorePoolSize(), ThreadConstants.THREAD_PREFIX_SCHEDULED);
  }

  /** 线程路由器 */
  @Bean
  public ThreadPoolRouter threadPoolRouter() {
    return new ThreadPoolRouter(playerExecutor(), serverExecutor());
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
}
