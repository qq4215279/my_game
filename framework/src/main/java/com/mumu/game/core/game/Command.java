package com.mumu.game.core.game;

import java.util.List;

/**
 * Command 命令
 * @author liuzhen
 * @version 1.0.0 2024/8/19 19:33
 */
public interface Command {

  /**
   * 执行命令
   * @param playerId 玩家id
   * @param args     参数
   * @since 2024/8/19 19:34
   */
  void execute(long playerId, List<String> args);

}
