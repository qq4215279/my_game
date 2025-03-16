package com.mumu.framework.core.log;

/**
 * CurrencyAction
 * 货币流通事件，用于日志
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:08
 */
public enum CurrencyAction {
  /** 绑定账号 */
  BIND_ACCOUNT,
  BalootCreateTable,
  /** 购买商品 */
  BUY_SHOP,
  /** 购买礼物商品 */
  BUY_GIFT_SHOP,
  /** GM */
  GM,
  /** baloot游戏失败消耗 */
  BALOOT_LOSE_GAME_COST,
  /** baloot游戏胜利获得 */
  BALOOT_WIN_GAME_REWARD,
  /** 使用互动表情 */
  BALOOT_USE_INTERACTIVI_EMOTE,
  /** 接收互动表情 */
  BALOOT_RECEIVE_INTERACTIVI_EMOTE,
  /** 创角发放默认道具 */
  CREATE_PLAYER_SEND_DEFAULT_ITEM,
  /** 玩家升级 */
  PLAYER_UPGRADE,
  /** 玩家VIP升级 */
  PLAYER_VIP_UPGRADE,
  /** 改名消耗 */
  UPDATE_NICK_COST,
  /** 使用道具 */
  USE_ITEM_COST,
  /** 邮件领奖 */
  MAIL,
  /** 任务 */
  TASK,
  /** 假购 */
  FAKE_CHARGE,
  /** 使用道具校验 */
  HAS_TIEM,
  /** 发送礼物 */
  SEND_GIFT,
  /** 接收礼物 */
  RECEIVE_GIFT,
  /** 出售礼物 */
  SELL_GIFT,
  /** 新手签到奖励 */
  NEW_PLAYER_SIGN,
  /** 比赛 */
  TOURNAMENT,
  /** 领取挑战币奖励时消耗 */
  CHALLENGE_CONSUME,
  /** 领取挑战币奖励 */
  CHALLENGE_REWARD,
  /** 冲级活动奖励 */
  UPGRADE_ACTIVIEY_REWARD,
  /** 赛季结束，清除所有道具 */
  SEASON_OVER_CLEAL_ITEMS,
  /** 修改机器人金币 */
  ROBOT_CHANGE_GOLD,
  /** 充值 */
  CHARGE,
  /** baloot游戏胜利经验加成活动额外获得 */
  BALOOT_WIN_GAME_EXP_ADDITION_REWARD,
  ;
}
