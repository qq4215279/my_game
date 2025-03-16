package com.mumu.framework.core.thread;

import com.google.common.collect.Maps;
import com.mumu.framework.core.log.LogTopic;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ScheduledExecutorUtil
 * 定时任务处理
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:56
 */
@Component
@SuppressWarnings("all")
public class ScheduledExecutorUtil {
  static ScheduledExecutorService scheduledExecutor;

  static final Map<String, ScheduledFuture<?>> scheduledFutureMap = Maps.newConcurrentMap();

  @Autowired
  public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
    ScheduledExecutorUtil.scheduledExecutor = scheduledExecutor;
  }

  /** 异步执行一次 */
  public static void execute(Runnable command) {
    scheduledExecutor.execute(wrapper(StringUtils.EMPTY, command, false));
  }

  /** 异步延迟执行一次 */
  public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return scheduledExecutor.schedule(wrapper(StringUtils.EMPTY, command, false), delay, unit);
  }

  /** 保证任务两次之间存在固定的延迟，不会因为任务耗时，导致并发执行 */
  public static ScheduledFuture<?> scheduleWithFixedDelay(
      String key, Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return scheduledFutureMap.computeIfAbsent(
        key,
        k ->
            scheduledExecutor.scheduleWithFixedDelay(
                wrapper(key, command, false), initialDelay, delay, unit));
  }

  /** 以固定速率执行，不会因为任务耗时，导致下次任务推辞执行 */
  public static ScheduledFuture<?> scheduleAtFixedRate(
      String key, Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return scheduledFutureMap.computeIfAbsent(
        key,
        k ->
            scheduledExecutor.scheduleAtFixedRate(
                wrapper(key, command, false), initialDelay, delay, unit));
  }

  /** 注册一个延迟任务 millis: 未来的时间戳 */
  public static ScheduledFuture<?> scheduleAt(String key, Runnable command, long millis) {
    long delay = Math.max( millis - System.currentTimeMillis(), 0L);
    return schedule(key, command, delay, TimeUnit.MILLISECONDS, true);
  }

  /** 注册一个延迟任务 delay: 延迟时间 */
  public static ScheduledFuture<?> schedule(
      String key, Runnable command, long delay, TimeUnit unit) {
    return schedule(key, command, delay, unit, true);
  }

  /** 注册一个延迟任务，isReplace：如果已经注册过，是否替换掉 */
  public static ScheduledFuture<?> schedule(
      String key, Runnable command, long delay, TimeUnit unit, boolean isReplace) {
    if (isReplace) cancel(key);
    return scheduledFutureMap.computeIfAbsent(
        key, k -> scheduledExecutor.schedule(wrapper(key, command), delay, unit));
  }

  /** 取消指定key的任务 */
  public static void cancel(String key) {
    cancel(key, false);
  }

  public static void cancel(String key, boolean force) {
    ScheduledFuture<?> future = scheduledFutureMap.remove(key);
    if (future != null) future.cancel(force);
  }

  public static Runnable wrapper(String key, Runnable task) {
    return wrapper(key, task, true);
  }

  /** 包装任务线程，打印线程报错日志 isCancelledAfterRun-执行后取消当前任务key */
  public static Runnable wrapper(String key, Runnable task, boolean isCancelledAfterRun) {
    return () -> {
      try {
        task.run();
      } catch (Exception e) {
        LogTopic.ACTION.error(e, "Scheduled-" + key);
      } finally {
        if (isCancelledAfterRun) cancel(key);
      }
    };
  }

  public static void destroy() {
    LogTopic.ACTION.info("容器关闭 ScheduledExecutor stop...");
    try {
      scheduledExecutor.shutdown();
      LogTopic.ACTION.info("容器关闭 ScheduledExecutor end");
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "容器关闭 ScheduledExecutor destroy error");
    }
  }
}
