package com.mumu.common.constants;

/**
 * ChargeConstants
 * 充值常量类
 * @author liuzhen
 * @version 1.0.0 2024/10/9 14:58
 */
public interface ChargeConstants {
  /** TODO world服地址 */
  String WORLD_SERVER_URL = "http://127.0.0.1:8388";
  /** TODO 写死 帐号服地址 account服地址确认发货url */
  String CONFIRMPURCHASE_SERVER_URL = "http://localhost:8000/charge/callback/confirmPurchase";

  /** TODO 写死 apple帐号服地址 account服地址确认发货url */
  String APPLE_CONFIRMPURCHASE_SERVER_URL = "http://localhost:8000/appleCharge/callback/confirmPurchase";

  /** TODO 写死 谷歌帐号服地址 account服地址确认发货url */
  String GOOGLE_CONFIRMPURCHASE_SERVER_URL = "http://localhost:8000/googleCharge/callback/confirmPurchase";


  /** 订单状态 - 初始化 */
  int INIT_CHARGE_STATE = 0;
  /** 订单状态 - 充值成功 */
  int INIT_CHARGE_SUCCESS = 1;
  /** 订单状态 - 游戏服发货成功 */
  int INIT_CHARGE_FINISH = 2;
  /** 订单状态 - 通知第三方发货成功 */
  int INIT_CHARGE_NOTIFY_THIRD_FINISH = 3;

}
