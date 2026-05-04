package com.mumu.game.core.lock;

import java.util.concurrent.TimeUnit;

/** 锁对象 @Date: 2025/7/16 下午3:35 @Author: xu.hai */
public enum Locker {
  /** 转盘抽奖 */
  TURNTABLE_DRAW_LOCK,
  ;

  /** 获取锁 */
  public InternalLock lock() {
    return LockUtil.lock(this);
  }

  /** 获取锁 */
  public InternalLock lock(long time, TimeUnit unit) {
    return LockUtil.lock(this, time, unit);
  }

}
