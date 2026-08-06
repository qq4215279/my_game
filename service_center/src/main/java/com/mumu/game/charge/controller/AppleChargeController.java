package com.mumu.game.charge.controller;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mumu.game.charge.service.AppleChargeServer;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogTopic;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AppleChargeController
 * 苹果充值Controller
 * @author liuzhen
 * @version 1.0.0 2025/1/14 16:54
 */
@RestController
@RequestMapping("/appleCharge")
public class AppleChargeController {
  /**log */
  private final static LogTopic log = LogTopic.ACTION;

  @Resource
  private AppleChargeServer appleChargeServer;

  /**
   * 苹果支付成功回调
   * @param request request
   * @param params params
   * @return com.game.http.core.HttpResult
   * @since 2025/1/14 16:58
   */
  @PostMapping("/callback/pay")
  public HttpResult pay(HttpServletRequest request, @RequestBody Map<String, String> params) {
    long playerId = Long.parseLong(params.get("playerId"));
    String transactionReceipt = params.get("transactionReceipt");
    // log.info(LogAction.APPLE_CHARGE_PAY, "收到请求", "playerId", playerId, "params", params);
    appleChargeServer.asyncPay(playerId, transactionReceipt);
    return HttpResult.success();
  }

  /**
   * 补单By通过查询历史记录
   * @param request request
   * @param params params 
   * @return com.game.http.core.HttpResult
   * @since 2025/2/28 16:43
   */
  @PostMapping("/callback/pay4History")
  public HttpResult pay4History(HttpServletRequest request, @RequestBody Map<String, String> params) {
    long playerId = Long.parseLong(params.get("playerId"));
    String transactionId = params.get("transactionId");

    long endDate = System.currentTimeMillis();
    String endDateStr = params.get("endDate");
    if (StringUtils.isNotEmpty(endDateStr)) {
      endDate = Long.parseLong(endDateStr);
    }

    long startDate = DateUtil.beginOfDay(new Date()).getTime() - TimeUnit.DAYS.toMillis(2);
    String startDateStr = params.get("startDate");
    if (StringUtils.isNotEmpty(startDateStr)) {
      startDate = Long.parseLong(startDateStr);
    }


    if (startDate >= endDate) {
      // 只查找3天内的订单
      startDate = DateUtil.beginOfDay(new Date()).getTime() - TimeUnit.DAYS.toMillis(2);
    }

    // 超过30天，默认查询10天
    if (TimeUnit.MILLISECONDS.toDays((endDate - startDate)) > 30) {
      startDate = DateUtil.beginOfDay(new Date()).getTime() - TimeUnit.DAYS.toMillis(10);
    }

    return appleChargeServer.pay4History(playerId, transactionId, startDate, endDate);
  }

  /**
   * 确认发货（游戏服发货成功回调，回调Order服务确认购买）
   * @param request request
   * @param playerId playerId
   * @param transactionId transactionId
   * @param sandbox sandbox
   * @return com.game.http.core.HttpResult
   * @since 2025/1/15 15:38
   */
  @GetMapping("/callback/confirmPurchase")
  public HttpResult confirmPurchase(HttpServletRequest request, @RequestParam String orderId, @RequestParam long playerId, @RequestParam String transactionId,
      @RequestParam boolean sandbox) {
    return appleChargeServer.confirmPurchase(orderId, playerId, transactionId, sandbox);
  }
}
