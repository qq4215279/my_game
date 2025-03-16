package com.mumu.framework.business.language.enums;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * MailLanguageEnum
 * 邮件国际化key枚举
 * @author liuzhen
 * @version 1.0.0 2024/12/31 14:47
 */
@ProtobufClass
public enum MailLanguageEnum {
  /** vip每日奖励 */
  VIP_DAILY_REWARD,
  /** 创角邮件 */
  PLAYER_CREATION,
  /** 比赛报名费退回邮件 */
  TOURNAMENT_RETURN_COST,
  /** 比赛结算邮件 */
  TOURNAMENT_SETTLE_REWARDS,
  ;


  /** 邮件标题国际化key前缀 */
  private static final String TITLE_PREFIX = "MAIL_TITLE_";
  /** 邮件内容国际化key前缀 */
  private static final String CONTENT_PREFIX = "MAIL_CONTENT_";

  /**
   * 邮件标题国际化key
   * @return java.lang.String
   * @date 2024/12/31 15:07
   */
  public String getTitleKey() {
    return TITLE_PREFIX + name();
  }

  /**
   * 邮件内容国际化key
   * @return java.lang.String
   * @date 2024/12/31 15:07
   */
  public String getContentKey() {
    return CONTENT_PREFIX + name();
  }

}
