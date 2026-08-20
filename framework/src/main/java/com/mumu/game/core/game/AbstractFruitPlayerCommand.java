package com.mumu.game.core.game;

import java.util.List;


import cn.hutool.core.lang.Pair;
import com.mumu.game.proto.message.core.ErrorCode;

/**
 * AbstractFruitPlayerCommand
 * 抽象水果机玩家命令
 * @author liuzhen
 * @version 1.0.0 2025/6/20 14:37
 */
public abstract class AbstractFruitPlayerCommand extends AbstractFruitCommand {

  public AbstractFruitPlayerCommand(FruitCommandEnum command) {
    super(command);
  }
  /**
   * 校验玩家能否执行操作
   * @param playerId 玩家id
   * @param args     参数
   * @return boolean
   * @since 2024/8/20 10:47
   */
  public Pair<Boolean, ErrorCode> checkExecute(long playerId, List<String> args) {
    return checkExecuteByDefault(playerId, args);
  }

  /**
   * 校验玩家能否执行操作
   * @param playerId 玩家id
   * @param args     参数
   * @return boolean
   * @since 2024/9/9 12:05
   */
  protected Pair<Boolean, ErrorCode> checkExecuteByDefault(long playerId, List<String> args) {
    // FruitPlayer fruitPlayer = fruitMachine.getFruitPlayer(playerId);
    // if (fruitPlayer == null) {
    //   return Pair.of(false, ErrorCode.FAIL_PLAYER_NOT_EXIST);
    // }

    return Pair.of(true, ErrorCode.SUCCESS);
  }

}
