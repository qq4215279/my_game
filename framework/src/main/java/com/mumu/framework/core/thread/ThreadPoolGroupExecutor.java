package com.mumu.framework.core.thread;

import com.mumu.common.constants.ThreadConstants;
import com.mumu.framework.util2.ThreadPoolUtil;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ThreadPoolGroupExecutor
 * 玩家队列线程池
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:57
 */
public class ThreadPoolGroupExecutor {
  private final ThreadPoolExecutor[] executors;

  /** 线程数 */
  private final int corePoolSize;

  /** 队列大小是二的N次方 */
  private final boolean isPowerOfTwo;

  public ThreadPoolGroupExecutor(int corePoolSize, int maxQueueSize) {
    this.corePoolSize = corePoolSize;
    this.isPowerOfTwo = (corePoolSize & -corePoolSize) == corePoolSize;
    this.executors = new ThreadPoolExecutor[corePoolSize];
    // 初始化线程池
    for (int i = 0; i < corePoolSize; i++) {
      executors[i] =
          ThreadPoolUtil.newSingleExecutor(
              ThreadConstants.THREAD_PREFIX_PLAYER + i + "-", maxQueueSize);
    }
  }

  /** 关闭线程组 */
  public void shutdown() {
    for (ThreadPoolExecutor executor : this.executors) {
      executor.shutdown();
    }
  }

  /** 添加任务 */
  public void addTask(long key, Runnable task) {
    executors[route(key)].execute(task);
  }

  /** 计算路由到哪一个线程队列中 */
  private int route(long key) {
    return (int) (isPowerOfTwo ? key & (corePoolSize - 1) : key % corePoolSize);
  }
}
