package com.mumu.common.constants;

/**
 * ChargeConstants
 * 充值常量类
 * @author liuzhen
 * @version 1.0.0 2024/10/9 14:58
 */
public interface ChargeConstants {


    /** 订单状态 - 初始化 */
    int INIT_CHARGE_STATE = 0;
    /** 订单状态 - 充值成功 */
    int INIT_CHARGE_SUCCESS = 1;
    /** 订单状态 - 游戏服发货成功 */
    int INIT_CHARGE_FINISH = 2;
    /** 订单状态 - 通知第三方发货成功 */
    int INIT_CHARGE_NOTIFY_THIRD_FINISH = 3;

}
