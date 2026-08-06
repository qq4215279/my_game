/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.log;

/**
 * LogAction
 * 日志Action模块
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:07
 */
public interface LogAction {
    /** 苹果支付 */
    String APPLE_CHARGE_PAY = "appleChargePay";

    /** 华为支付 */
    String HUAWEI_CHARGE_PAY = "huaweiChargePay";

    /** 谷歌支付 */
    String GOOGLE_CHARGE_PAY = "googleChargePay";

    /** 第三方支付 */
    String XSOLLA_CHARGE_PAY = "xsollaChargePay";

    /** 账号登陆 */
    String ACCOUNT = "accountAction";

    /** 接口异常处理 */
    String CONTROLLER_ERROR = "controllerError";
}
