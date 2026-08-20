package com.mumu.game.core.game;

/**
 * FruitCommand
 * 水果机命令
 * @author liuzhen
 * @version 1.0.0 2025/6/20 14:33
 */
public interface FruitCommand extends Command {

  /**
   * 构建并推送消息
   * @param playerId playerId
   * @param args args
   * @since 2025/6/23 16:32
   */
  void buildAndNotifyMsg(long playerId, Object... args);
}
