package com.mumu.game.core.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Joiner;

import com.mumu.game.core.log.LogAction;
import com.mumu.game.core.log.LogTopic;
import lombok.Getter;

/**
 * InternalLock
 * 锁
 * @author liuzhen
 * @version 1.0.0 2025/5/28 15:48
 */
public class InternalLock implements AutoCloseable {
  /** 获取锁的超时时间，单位：s */
  static final int LOCK_TIME_OUT = 5;

  /** 锁 */
  private final Lock lock = new ReentrantLock();
  /** 当前持有锁的线程 */
  @Getter
  private Thread holdThread;

  /**
   * 尝试获取锁
   * @since 2025/5/28 17:00
   */
  public void lock() {
    lock(LOCK_TIME_OUT, TimeUnit.SECONDS);
  }

  /**
   * 尝试获取锁
   * @param time time
   * @param unit unit
   * @since 2025/7/16 15:05
   */
  public void lock(long time, TimeUnit unit) {
    boolean success = false;
    try {
      success = lock.tryLock(time, unit);
    } catch (InterruptedException ignored) {
    }

    if (success) {
      holdThread = Thread.currentThread();
    } else {
      // 堆栈信息
      String currentStack = LogTopic.getStackTrace();
      String holdStack = LogTopic.getStackTrace(holdThread);

      LogTopic.ACTION.error(LogAction.JVM_LOCK, " =========== 获取InternalLock超时，可能发生死锁=========== ",
          "【当前线程】堆栈：", Thread.currentThread(), " ", currentStack, " 【持有锁线程】堆栈：", holdStack, " ", holdStack);
      throw new RuntimeException("获取InternalLock超时，可能发生死锁");
    }
  }

  /** 获取堆栈信息 */
  public static String getThreadStackTrace(Thread thread) {
    return Joiner.on("\n").join(thread.getStackTrace());
  }
  
  /**
   * 被指定线程持有锁
   * @return boolean
   * @since 2025/7/16 15:58
   */
  public boolean holdLock() {
    return holdThread != null;
  }

  @Override
  public void close() {
    holdThread = null;
    lock.unlock();
  }
}
