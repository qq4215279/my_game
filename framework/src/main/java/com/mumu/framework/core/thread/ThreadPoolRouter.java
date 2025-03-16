package com.mumu.framework.core.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

/**
 * ThreadPoolRouter
 * 线程选择器
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:59
 */
public class ThreadPoolRouter {
  /** 统计 - 添加玩家任务总数 */
  final LongAdder playerTaskCount = new LongAdder();

  /** 统计 - 添加服务任务总数 */
  final LongAdder serverTaskCount = new LongAdder();

  /** 统计 - 完成的任务总数 */
  final LongAdder processTaskCount = new LongAdder();

  /** 玩家线程 */
  final ThreadPoolGroupExecutor playerExecutor;

  /** 服务线程 */
  final ThreadPoolExecutor serverExecutor;

  static final int MAX_QUEUE_SIZE = 5000;

  final LinkedBlockingQueue<Worker> workPool = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

  public ThreadPoolRouter(
      ThreadPoolGroupExecutor playerExecutor, ThreadPoolExecutor serverExecutor) {
    this.playerExecutor = playerExecutor;
    this.serverExecutor = serverExecutor;
    // 初始化任务队列池
    for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
      workPool.add(newWorker());
    }
  }

  private Worker newWorker() {
    return new Worker(workPool, processTaskCount);
  }

  private Worker getWorker() {
    Worker worker = workPool.poll();
    return worker != null ? worker : newWorker();
  }

  /** TODO 消息路由 */
  // public void autoExecute(MessageContext context, Runnable task) {
  //   autoExecute(context.getRouteId(), context.getCmdCode(), task);
  // }

  /** 消息路由 */
  public void autoExecute(Long key, int cmd, Runnable task) {
    Worker worker = getWorker();
    worker.setCmd(cmd);
    worker.setTask(task);
    worker.begin();
    if (key == null) {
      serverExecutor.execute(worker);
      serverTaskCount.increment();
    } else {
      playerExecutor.addTask(key, worker);
      playerTaskCount.increment();
    }
  }
}
