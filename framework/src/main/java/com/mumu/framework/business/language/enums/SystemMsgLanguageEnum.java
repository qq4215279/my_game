package com.mumu.framework.business.language.enums;

/**
 * SystemMsgLanguageEnum
 * 系统消息国际化枚举
 * @author liuzhen
 * @version 1.0.0 2024/12/31 14:46
 */
public enum SystemMsgLanguageEnum {
  /** 玩家创角消息 */
  CREATE_PLAYER,
  /** 发送礼物 */
  SEND_GIFT,
  ;

  /** 国际化key前缀 */
  private static final String PREFIX = "SYS_MSG_";

  SystemMsgLanguageEnum() {
  }

  /**
   * 获取系统国际化key
   * @return java.lang.String
   * @date 2024/12/31 15:05
   */
  public String getSysMsgKey() {
    return PREFIX + name();
  }

}
