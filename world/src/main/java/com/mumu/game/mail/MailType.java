package com.mumu.game.mail;

/** 邮件类型 @Date: 2024/9/2 下午4:06 @Author: xu.hai */
public interface MailType {
  /** 系统邮件 */
  int SYSTEM = -1;
  /** GM邮件 */
  int GM = 0;
  /** vip系统邮件 */
  int VIP = 1;
  /** 创角邮件 */
  int CREATE_PLAYER = 2;
  /** BET下注小游戏 */
  int BET = 3;
  /** 发送礼物 */
  int SEND_GIFT = 4;
  /** 比赛邮件 */
  int TOURNAMENT = 5;
  /** 退还自建房的房卡 */
  int RETURN_DIY_TABLE_COST = 6;
  /** 未绑定用户充值提示邮件 */
  int UNBIND_USER_CHARGE_TIP = 7;
  /** 排行榜奖励 */
  int RANK = 8;
  /** 水果机小游戏 */
  int FRUIT = 9;
  /** 神灯小游戏 */
  int GENIE_LAMP = 10;
}
