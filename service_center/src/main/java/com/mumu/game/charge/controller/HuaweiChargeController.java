package com.mumu.game.charge.controller;

import java.util.Map;

import com.mumu.game.charge.service.HuaweiChargeServer;
import com.mumu.game.http.HttpResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ChargeController
 * 华为充值
 * @author liuzhen
 * @version 1.0.0 2024/11/26 10:10
 */
@RestController
@RequestMapping("/charge")
public class HuaweiChargeController {
  @Resource
  private HuaweiChargeServer huaweiChargeServer;

  /**
   * 支付回调
   * @param request request
   * @param params params
   * @return com.game.account.vo.R
   * @since 2024/11/28 10:34
   */
  @PostMapping("/callback/pay")
  public HttpResult pay(HttpServletRequest request, @RequestBody Map<String, String> params) {
    String inAppPurchaseData = params.get("inAppPurchaseData");
    String inAppSignature = params.get("inAppSignature");
    String signatureAlgorithm = params.get("signatureAlgorithm");

    return huaweiChargeServer.pay(inAppPurchaseData, inAppSignature, signatureAlgorithm);
  }

  /**
   * 确认发货（游戏服发货成功回调，回调Order服务确认购买）
   * @param request request
   * @param purchaseToken purchaseToken
   * @param productId productId
   * @param accessToken accessToken
   * @return com.game.account.vo.R
   * @since 2024/11/26 20:06
   */
  @GetMapping("/callback/confirmPurchase")
  public HttpResult confirmPurchase(HttpServletRequest request, @RequestParam String orderId,
      @RequestParam String purchaseToken, @RequestParam String productId, @RequestParam String accessToken) {
    return huaweiChargeServer.confirmPurchase(orderId, purchaseToken, productId, accessToken);
  }
}
