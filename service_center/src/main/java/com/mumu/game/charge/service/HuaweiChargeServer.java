package com.mumu.game.charge.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.mumu.game.charge.config.HuaweiChargeConfig;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.charge.util.HuaweiPayHelper;
import com.mumu.game.http.HttpCode;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogAction;
import com.mumu.game.log.LogTopic;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;

/**
 * ChargeServer
 * 支付server
 * @author liuzhen
 * @version 1.0.0 2024/11/26 10:11
 */
@Service
public class HuaweiChargeServer {
  /**log */
  private final static LogTopic log = LogTopic.ACTION;

  @Resource
  private HuaweiChargeConfig huaweiChargeConfig;
  @Resource
  private ChargeInfoDao chargeInfoDao;

  /**
   * 支付
   * @param inAppPurchaseData inAppPurchaseData
   * @param inAppSignature inAppSignature
   * @param signatureAlgorithm signatureAlgorithm
   * @return com.game.account.vo.R
   * @since 2024/11/26 10:33
   */
  public HttpResult pay(String inAppPurchaseData, String inAppSignature, String signatureAlgorithm) {
    log.info(LogAction.HUAWEI_CHARGE_PAY, "callbackPay",
        "inAppPurchaseData", inAppPurchaseData, "inAppSignature", inAppSignature, "signatureAlgorithm", signatureAlgorithm);

    if (StringUtils.isEmpty(inAppPurchaseData) || StringUtils.isEmpty(inAppSignature)) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#参数为空");
      return HttpResult.error(HttpCode.FAIL, "参数为空");
    }

    // 1. inAppPurchaseData 校验
    if (!HuaweiPayHelper.checkSign(inAppPurchaseData, inAppSignature, huaweiChargeConfig.getPublicKey(), signatureAlgorithm)) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkSign fail#inAppPurchaseData");
      return HttpResult.error(HttpCode.FAIL, "checkSign fail");
    }

    JSONObject inAppPurchaseDataJson = new JSONObject(inAppPurchaseData);
    // 华为支付订单ID
    String huaweiOrderId = inAppPurchaseDataJson.getStr("orderId");
    // 商品ID
    String productId = inAppPurchaseDataJson.getStr("productId");
    String purchaseToken = inAppPurchaseDataJson.getStr("purchaseToken");
    long price = inAppPurchaseDataJson.getLong("price");

    // 2. Order服务，获取accessToken
    String accessToken = "";
    try {
      accessToken = HuaweiPayHelper.getAppAccessToken(huaweiChargeConfig.getTOKEN_URL(),
          huaweiChargeConfig.getCLIENT_ID(), huaweiChargeConfig.getCLIENT_SECRET());
    } catch (Exception e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken fail");
      return HttpResult.error(HttpCode.FAIL, "get accessToken fail");
    } finally {
      log.info(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#accessToken", accessToken);
    }

    // 3. Order服务验证购买token
    try {
      if (!checkToken(purchaseToken, productId, accessToken)) {
        log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken fail");
        return HttpResult.error(HttpCode.FAIL, "checkToken fail");
      }
    } catch (Exception e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken fail");
      return HttpResult.error(HttpCode.FAIL, "checkToken fail");
    }

    // 4. 游戏服订单校验
    // 订单id（游戏商品订单）
    String orderId = inAppPurchaseDataJson.getStr("developerPayload");
    try {
      if (!checkOrder(orderId, productId, price)) {
        log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder fail");
        return HttpResult.error(HttpCode.FAIL, "checkOrder fail");
      }
    } catch (Exception e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder fail");
    }

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    chargeInfo.setState(ChargeConstants.INIT_CHARGE_SUCCESS);
    chargeInfo.setChannelOrderId(huaweiOrderId);
    chargeInfoDao.save(chargeInfo);

    // 5. 游戏服发货
    boolean paySuccess = true;
    try {
      if (!doPay(orderId, huaweiOrderId, purchaseToken, productId, accessToken)) {
        paySuccess = false;
        log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#doPay fail");
      }
    } catch (Exception e) {
      paySuccess = false;
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#doPay fail");
    }

    if (!paySuccess) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#payFail");
      return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
    }

    return HttpResult.success();
  }

  /**
   * 游戏服校验订单
   * @param orderId orderId
   * @param productId productId
   * @param price price
   * @return boolean
   * @since 2024/11/26 16:29
   */
  private boolean checkOrder(String orderId, String productId, long price) {
    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#chargeInfo is not exist");
      return false;
    }

    // 0: 初始化; 1: 充值成功; 2: 发货成功
    if (chargeInfo.getState() == ChargeConstants.INIT_CHARGE_FINISH) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder fail#has pay done!");
      return false;
    }

    if (!Objects.equals(chargeInfo.getProductId(), productId)) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder fail#check productId fail");
      return false;
    }

    // TODO 创建时订单金额 与 华为订单金额 校验。待解决华为汇率转换后金额不一致问题
    /*if (chargeInfo.getPrice() != price) {
      log.error("callbackPay#checkOrder fail#check price fail");
      return false;
    }*/

    return true;
  }

  /**
   * 请求游戏服发货
   * @param orderId orderId
   * @param huaweiOrderId huaweiOrderId
   * @param purchaseToken purchaseToken
   * @param productId productId
   * @param accessToken accessToken
   * @return boolean
   * @since 2024/11/26 16:52
   */
  private boolean doPay(String orderId, String huaweiOrderId, String purchaseToken, String productId, String accessToken) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("orderId", orderId);
    paramMap.put("huaweiOrderId", huaweiOrderId);
    paramMap.put("purchaseToken", purchaseToken);
    paramMap.put("productId", productId);
    paramMap.put("accessToken", accessToken);

    try {
      String result = HttpUtil.get(ChargeConstants.WORLD_SERVER_URL + "/charge/chargeByHuawei", paramMap);
      log.info(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder#getOrderInfo#result", result);

      JSONObject resJson = new JSONObject(result);
      int code = resJson.getInt("code");
      if (code != 0) {
        log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder#getOrderInfo fail#code", code);
        return false;
      }

    } catch (Exception e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkOrder#getOrderInfo fail");
      return false;
    }

    return true;
  }

  /**
   * token 验证
   * @param purchaseToken purchaseToken
   * @param productId productId
   * @param accessToken accessToken
   * @return boolean
   * @since 2024/11/26 15:59
   */
  private boolean checkToken(String purchaseToken, String productId, String accessToken) throws Exception {
    // pack the request body
    Map<String, String> bodyMap = new HashMap<>();
    bodyMap.put("purchaseToken", purchaseToken);
    bodyMap.put("productId", productId);
    // 将 Map 转换为 JSON 字符串
    String msgBody = JSONUtil.toJsonStr(bodyMap);

    String response = null;
    try {
      HttpResponse httpResponse = HttpRequest.post(
          huaweiChargeConfig.getTOBTOC_SITE_URL() + "/applications/purchases/tokens/verify")
      .headerMap(HuaweiPayHelper.buildAuthorization(accessToken), false)
      .body(msgBody)
      .execute();
      response = httpResponse.body();
    } catch (HttpException e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken fail#remote verify fail");
    }

    log.info(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken response", response);

    JSONObject responseJson = new JSONObject(response);
    String responseCode = responseJson.getStr("responseCode");
    if (!"0".equals(responseCode)) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkToken fail#responseCode", responseCode);
      return false;
    }

    // 校验返回
    String purchaseTokenData = responseJson.getStr("purchaseTokenData");
    String dataSignature = responseJson.getStr("dataSignature");
    String signatureAlgorithm = responseJson.getStr("signatureAlgorithm");

    if (!HuaweiPayHelper.checkSign(purchaseTokenData, dataSignature, huaweiChargeConfig.getPublicKey(), signatureAlgorithm)) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "callbackPay#checkSign fail#purchaseTokenData");
      return false;
    }

    return true;
  }

  /**
   * 回调Order服务确认购买
   * @param purchaseToken purchaseToken
   * @param productId productId
   * @param accessToken accessToken
   * @since 2024/11/26 16:00
   */
  public HttpResult confirmPurchase(String orderId, String purchaseToken, String productId, String accessToken) {
    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null || chargeInfo.getState() != ChargeConstants.INIT_CHARGE_FINISH) {
      log.error(LogAction.HUAWEI_CHARGE_PAY, "huaweiConfirmPurchase fail, 游戏服未发货, orderId", orderId);
      return HttpResult.error(HttpCode.FAIL, "游戏服未发货");
    }

    log.info(LogAction.HUAWEI_CHARGE_PAY,
        "confirmPurchase", orderId, "purchaseToken", purchaseToken, "productId", productId, "accessToken", accessToken);

    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("purchaseToken", purchaseToken);
    bodyMap.put("productId", productId);
    String msgBody = com.alibaba.fastjson.JSONObject.toJSONString(bodyMap);

    String response = null;
    try {
      HttpResponse httpResponse = HttpRequest.post(
              huaweiChargeConfig.getTOBTOC_SITE_URL() + "/applications/v2/purchases/confirm")
          .headerMap(HuaweiPayHelper.buildAuthorization(accessToken), false)
          .body(msgBody)
          .execute();
      response = httpResponse.body();
    } catch (Exception e) {
      log.error(e, LogAction.HUAWEI_CHARGE_PAY, "callbackPay#confirmPurchase fail");
    }

    log.info(LogAction.HUAWEI_CHARGE_PAY, "confirmPurchase#response", response);

    JSONObject responseJson = new JSONObject(response);
    String responseCode = responseJson.getStr("responseCode");
    if ("0".equals(responseCode)) {
      chargeInfo.setState(ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH);
      chargeInfoDao.save(chargeInfo);
    }

    return HttpResult.success();
  }

}
