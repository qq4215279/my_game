package com.mumu.game.charge.controller;

import com.mumu.game.charge.service.ChargeService;
import com.mumu.game.http.HttpResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ChargeController
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 13:49
 */
@RestController
@RequestMapping("/charge")
public class ChargeController {

    @Resource
    private ChargeService chargeService;

    /**
     * 标记订单支付失败
     * @param request request
     * @param orderId 订单id
     * @param errorInfo 失败信息
     * @param failType 失败类型: 1-主动取消; 2-其他失败(pay failed); 3-华为失败
     * @return com.mumu.game.http.HttpResult
     * @since 2026/8/2 14:02
     */
    @GetMapping("/markOrderFail")
    public HttpResult markOrderFail(HttpServletRequest request, String orderId, String errorInfo, int failType) {
        return chargeService.markOrderFail(orderId, errorInfo, failType);
    }
}
