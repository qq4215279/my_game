package com.mumu.game.charge.controller;

import java.util.Map;

import com.mumu.game.charge.service.GoogleChargeServer;
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
 * GoogleChargeController
 * 谷歌充值Controller
 * @author liuzhen
 * @version 1.0.0 2025/2/12 14:53
 */
@RestController
@RequestMapping("/googleCharge")
public class GoogleChargeController {

  @Resource
  private GoogleChargeServer googleChargeServer;

  /**
   * 谷歌支付成功回调
   * @param request request
   * @param params params
   * @return com.game.http.core.HttpResult
   * @since 2025/1/14 16:58
   */
  @PostMapping("/callback/pay")
  public HttpResult pay(HttpServletRequest request, @RequestBody Map<String, String> params) {
    long playerId = Long.parseLong(params.get("playerId"));
    String productId = params.get("productId");
    String purchaseToken = params.get("purchaseToken");

    return googleChargeServer.pay(playerId, productId, purchaseToken);
  }

  /**
   * 浏览器将被重定向到带有code 参数的重定向URI，该参数看起来类似于4/eWdxD7b-YSQ5CNNb-c2iI83KQx19.wp6198ti5Zc7dJ3UXOl0T3aRLxQmbwI
   * @param request request
   * @param code code 
   * @return com.game.http.core.HttpResult
   * @since 2025/2/14 15:38
   */
  @GetMapping("/callback/redirect")
  public HttpResult redirect(HttpServletRequest request, @RequestParam String code) {
    return googleChargeServer.redirect(request, code);
  }

  /**
   * 请求获取refreshToken
   * @param request request
   * @param code code
   * @return com.game.http.core.HttpResult
   * @since 2025/2/13 18:26
   */
  @GetMapping("/callback/getGoogleRefreshToken")
  public HttpResult getGoogleRefreshToken(HttpServletRequest request, @RequestParam String code) {
    return googleChargeServer.getGoogleRefreshToken(code);
  }

  /**
   * 确认发货（游戏服发货成功回调，回调Order服务确认购买）
   * @param request request
   * @param playerId playerId
   * @param productId productId
   * @param purchaseToken purchaseToken
   * @return com.game.http.core.HttpResult
   * @since 2025/2/12 17:04
   */
  @GetMapping("/callback/confirmPurchase")
  public HttpResult confirmPurchase(HttpServletRequest request, @RequestParam String orderId,
      @RequestParam long playerId, @RequestParam String productId, @RequestParam String purchaseToken) {
    return googleChargeServer.confirmPurchase(orderId, playerId, productId, purchaseToken);
  }
}
