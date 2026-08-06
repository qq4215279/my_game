package com.mumu.game.charge.service;

import com.mumu.game.http.HttpResult;

/**
 * ChargeServer
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 13:50
 */
public interface ChargeService {

    /**
     * 标记订单支付失败
     * @param orderId 订单id
     * @param errorInfo 失败信息
     * @param failType 失败类型: 1-主动取消; 2-其他失败(pay failed); 3-华为失败
     * @return com.mumu.game.http.HttpResult
     * @since 2026/8/2 14:02
     */
    HttpResult markOrderFail(String orderId, String errorInfo, int failType);
}
