package com.mumu.game.core.game;

/**
 * Updatable
 *
 * @author liuzhen
 * @version 1.0.0 2025/6/20 18:14
 */
public interface Updatable {

  /** 执行频率 ms */
  default int delay() {
    return 500;
  }

  /**
   * 帧更新
   *
   * @param now now
   * @since 2025/6/20 18:14
   */
  void update(long now);

  /** 销毁时执行 */
  default void destroy() {}
}
