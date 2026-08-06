package com.mumu.game.charge.enums;

/**
 * XsollaWebhookEnum
 * xsolla webhook 枚举
 * @author liuzhen
 * @version 1.0.0 2025/6/10 17:18
 */
public enum XsollaWebhookEnum {
  /** 用户验证 */
  USER_VALIDATION("user_validation"),
  /** 订单支付完成 */
  ORDER_PAID("order_paid"),
  ;

  /** webhook 类型 */
  private final String notificationType;


  XsollaWebhookEnum(String notificationType) {
    this.notificationType = notificationType;
  }

  /**
   * 命中webhook
   * @param notificationType notificationType
   * @return boolean
   * @since 2025/6/10 17:54
   */
  public boolean hitWebhook(String notificationType) {
    return this.notificationType.equals(notificationType);
  }

}
