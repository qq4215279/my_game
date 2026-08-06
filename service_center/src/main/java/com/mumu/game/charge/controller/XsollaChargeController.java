package com.mumu.game.charge.controller;

import com.mumu.game.charge.dto.XsollaPurchaseVO;
import com.mumu.game.charge.service.XsollaChargeServer;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogAction;
import com.mumu.game.log.LogTopic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSONObject;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * XsollaChargeController 艾克索拉（第三方支付）controller
 *
 * @author liuzhen
 * @version 1.0.0 2025/6/10 13:58
 */
@RestController
@RequestMapping("/xsollaCharge")
public class XsollaChargeController {

  @Resource private XsollaChargeServer xsollaChargeServer;

  /** xsolla 的用户查询 */
  @PostMapping("/userCheck")
  public ResponseEntity<Object> xsollaUserCheck(@RequestBody JSONObject body) {
    ResponseEntity<Object> result = null;
    try {
      return result = xsollaChargeServer.xsollaUserCheck(body);
    } finally {
      LogTopic.ACTION.info(
          LogAction.XSOLLA_CHARGE_PAY, "xsollaUserCheck", "body", body, "result", result);
    }
  }

  /**
   * 第三方支付成功回调
   *
   * @param request request
   * @param body body
   * @return java.util.Map<java.lang.String, java.lang.Object>
   * @since 2025/6/10 14:04
   */
  @PostMapping("/callback/webhook")
  public ResponseEntity<Object> fireWebhook(
      HttpServletRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody String body) {

    ResponseEntity<Object> result = null;
    try {
      return result = xsollaChargeServer.fireWebhook(authorization, body);
    } finally {
      LogTopic.ACTION.info(
          LogAction.XSOLLA_CHARGE_PAY,
          "fireWebhookFinally",
          "authorization",
          authorization,
          "body",
          body,
          "result",
          result);
    }
  }

  /**
   * 确认发货回调
   *
   * @param xsollaPurchaseVO xsollaPurchaseVO
   * @return com.game.http.core.HttpResult
   * @since 2025/6/12 13:41
   */
  @PostMapping("/callback/confirmPurchase")
  public HttpResult confirmPurchase(@RequestBody XsollaPurchaseVO xsollaPurchaseVO) {
    return xsollaChargeServer.confirmPurchase(xsollaPurchaseVO);
  }
}
