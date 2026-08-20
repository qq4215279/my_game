package com.mumu.game.core.game;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;


/**
 * FruitCommandEnum
 * 水果机命令枚举
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
public enum FruitCommandEnum {
  /** 游戏数据同步(进入) */
  FRUIT_SYNC,
  /** 进入下注状态 */
  FRUIT_ENTER_BETTING_STATE,
  /** 下注 arg0: fruitType; arg1: goldNum */
  FRUIT_BETTING,
  /** 自动下注 参数0: fruitType:goldNum1,goldNum2;fruitType:goldNum1,goldNum2 */
  FRUIT_AUTO_BETTING,
  /** 公布额外加倍 */
  FRUIT_PUBLIC_EXTRA_MULTI,
  /** 结算 */
  FRUIT_SETTLE,
  /** 新开盘 */
  FRUIT_NEW_CIRCLE,
  /** 游戏退出 */
  FRUIT_EXIT,
}