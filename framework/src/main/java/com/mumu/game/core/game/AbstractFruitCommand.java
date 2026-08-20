package com.mumu.game.core.game;

import com.mumu.game.core.log.LogTopic;
import jakarta.annotation.Resource;
import lombok.Getter;

/**
 * AbstractFruitCommand
 * 抽象水果机命令类
 * @author liuzhen
 * @version 1.0.0 2025/6/20 14:35
 */
public abstract class AbstractFruitCommand implements FruitCommand {

  protected final static LogTopic log = LogTopic.ACTION;

  /** 命令 */
  @Getter
  protected FruitCommandEnum command;
  /** 水果机 */
  // @Resource
  // protected FruitMachine fruitMachine;

  public AbstractFruitCommand(FruitCommandEnum command) {
    this.command = command;
  }

  @Override
  public void buildAndNotifyMsg(long playerId, Object... args) {
    // OnZCPushFruitGameMessage pushMsg = this.buildPushMsg(playerId, args);
    // FruitMsgUtil.notify(pushMsg);
  }

  /** 构建推送必要信息 */
  // protected OnZCPushFruitGameMessage buildPushMsg(long playerId, Object... args) {
  //   return FruitMsgUtil.buildBasePushMsg(fruitMachine, command, args);
  // }

}
