package com.mumu.framework.core.thread;

import com.mumu.framework.core.log.LogTopic;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.LongAdder;
import lombok.Setter;

/**
 * Worker
 * 任务列表
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:59
 */
public class Worker implements Runnable {
  @Setter
  Runnable task;

  @Setter int cmd;

  /** 记录开始时间 */
  long recordNanoTime;

  /** 统计 - 完成的任务总数 */
  final LongAdder processTaskCount;

  /** 工作池 */
  final LinkedBlockingQueue<Worker> workPool;

  public Worker(LinkedBlockingQueue<Worker> workPool, LongAdder processTaskCount) {
    this.workPool = workPool;
    this.processTaskCount = processTaskCount;
  }

  @Override
  public void run() {
    try {
      processTaskCount.increment();
      int count = processTaskCount.intValue();

      // 任务等待延迟
      slowLog("wait", recordNanoTime, count);

      // 执行任务
      long beginExecTime = System.nanoTime();
      task.run();
      // 任务执行延迟
      slowLog("exec", beginExecTime, count);
    } catch (Throwable e) {
      LogTopic.NET.error(e, "CmdWorker", "cmd", cmd);
    } finally {
      clear();
    }
  }

  private void slowLog(String action, long begin, int count) {
    long diff = (System.nanoTime() - begin) / 1000;
    if (diff > 500) {
      LogTopic.NET.warn(
          "CmdWorker slowLog",
          action,
          "cmdCode",
          cmd,
          // "cmd",
          // Cmd.get(cmd),
          "cost",
          diff,
          "count",
          count);
    }
  }

  public void begin() {
    recordNanoTime = System.nanoTime();
  }

  public void clear() {
    task = null;
    cmd = 0;
    recordNanoTime = 0;
    if (workPool.remainingCapacity() > 0) {
      workPool.add(this);
    }
  }

}
