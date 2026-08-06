package com.mumu.game.charge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.mumu.game.account.dao.AccountDao;
import com.mumu.game.account.entity.AccountEntity;
import com.mumu.game.account.entity.XsollaChargeInfo;
import com.mumu.game.charge.conf.ConfigPayID;
import com.mumu.game.charge.config.XsollaChargeConfig;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.XsollaChargeInfoDao;
import com.mumu.game.charge.dto.XsollaGoodsVO;
import com.mumu.game.charge.dto.XsollaPurchaseVO;
import com.mumu.game.charge.enums.XsollaWebhookEnum;
import com.mumu.game.charge.luban.ShopConfigManager;
import com.mumu.game.charge.util.RestUtil;
import com.mumu.game.constants.Symbol;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogAction;
import com.mumu.game.log.LogTopic;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.util.NumberUtil;
import jakarta.annotation.Resource;

/**
 * XsollaChargeServer 艾克索拉（第三方支付）server
 *
 * @author liuzhen
 * @version 1.0.0 2025/6/10 13:59
 */
@Service
public class XsollaChargeServer {
  /** 未完成支付状态 */
  private static final int XSOLLA_UN_FINISH_STATE = 0;

  /** 艾克索拉（第三方支付） - 完成支付状态 */
  private static final int XSOLLA_FINISH_STATE = 1;

  /** 订单检查cd */
  private static final long CHECK_CD = TimeUnit.MINUTES.toMillis(30);

  /** log */
  private static final LogTopic log = LogTopic.ACTION;

  @Resource
  AccountDao accountDao;
  // @Resource private AccountServer accountServer;
  @Resource private XsollaChargeConfig xsollaChargeConfig;
  @Resource private XsollaChargeInfoDao xsollaChargeInfoDao;

  /**
   * 保底检查第三方订单发货情况
   *
   * @since 2025/6/12 13:49
   */
  @Scheduled(cron = "0 0/10 * * * ? ")
  public void checkXsollaChargeInfo() {
    Date date = new Date();

    // 未成功发货订单
    List<XsollaChargeInfo> chargeInfoList =
        xsollaChargeInfoDao.getChargeInfoListByState(XSOLLA_UN_FINISH_STATE);
    for (XsollaChargeInfo xsollaChargeInfo : chargeInfoList) {
      if (date.getTime() < xsollaChargeInfo.getLastCheckTime().getTime() + CHECK_CD) {
        continue;
      }

      // TODO 玩家不在线过滤

      xsollaChargeInfo.setLastCheckTime(date);
      xsollaChargeInfoDao.updateById(xsollaChargeInfo);

      // 异步发货
      long playerId = xsollaChargeInfo.getPlayerId();
      String goodsInfos = xsollaChargeInfo.getGoodsInfos();
      log.info(
          LogAction.XSOLLA_CHARGE_PAY,
          "check2AsyncPay",
          "playerId",
          playerId,
          "channelOrderId",
          xsollaChargeInfo.getId(),
          "goodsInfos",
          goodsInfos);
      List<XsollaGoodsVO> goodsDtoList = convert2GoodsDto(goodsInfos);
      asyncPay(playerId, xsollaChargeInfo.getId(), xsollaChargeInfo.getMode(), goodsDtoList);
    }
  }

  /** 登陆时的用户校验 */
  public ResponseEntity<Object> xsollaUserCheck(JSONObject body) {
    Map<String, Object> map = checkUser(body);
    return map != null
        ? ResponseEntity.status(HttpStatus.OK).body(map)
        : ResponseEntity.notFound().build();
  }

  /**
   * 用户登录验证
   * @param body body
   * @return java.util.Map<java.lang.String,java.lang.Object>
   * @since 2025/6/10 17:54
   */
  private Map<String, Object> checkUser(JSONObject body) {
    String xsollaId =
        Optional.ofNullable(body.getJSONObject("user"))
            .map(u -> u.getString("id"))
            .orElse(null);

    if (xsollaId == null) return null;
    if (!NumberUtil.isNumber(xsollaId)) return null;

    AccountEntity account = accountDao.getAccountEntity(Long.parseLong(xsollaId));
    // AccountVo account = accountServer.getAccountByToken(xsollaId);
    if (account == null) return null;

    String id = String.valueOf(account.getId());
    return Map.of("user", Map.of("id", id, "name", id));
  }

  /**
   * 处理webhook
   *
   * @param authorization authorization
   * @param body body中的原始json字符串
   * @return java.util.Map<java.lang.String,java.lang.Object>
   * @since 2025/6/10 17:55
   */
  public ResponseEntity<Object> fireWebhook(String authorization, String body) {
    JSONObject json = JSONObject.parseObject(body);
    if (json == null) {
      log.error(
          LogAction.XSOLLA_CHARGE_PAY,
          "fireWebhook",
          "body is null",
          "authorization",
          authorization,
          "json",
          json);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // webhook 类型
    String notificationType = json.getString("notification_type");
    if (StringUtils.isBlank(notificationType)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // 1. 登录验证
    if (XsollaWebhookEnum.USER_VALIDATION.hitWebhook(notificationType)) {
      return fireUserValidationWebhook(json);

      // 2. 订单支付完成，验证发货
    } else if (XsollaWebhookEnum.ORDER_PAID.hitWebhook(notificationType)) {
      return fireOrderPaidWebhook(authorization, json, body);

    } else {
      log.error(
          LogAction.XSOLLA_CHARGE_PAY,
          "fireWebhook",
          "Not Implemented",
          "authorization",
          authorization,
          "notificationType",
          notificationType,
          "json",
          json);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  /** 1. 支付前的UserValidation的用户校验 */
  public ResponseEntity<Object> fireUserValidationWebhook(JSONObject body) {
    long playerId = Long.parseLong(Optional.ofNullable(body.getJSONObject("user"))
        .map(u -> u.getString("id")).orElse("0L"));
    AccountEntity accountEntity = accountDao.getAccountEntity(playerId);

    return accountEntity != null ? ResponseEntity.noContent().build()
        : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  /**
   * 2. 订单验证
   *
   * @param authorization authorization
   * @param json body
   * @return java.util.Map<java.lang.String,java.lang.Object>
   * @since 2025/6/10 17:54
   */
  private ResponseEntity<Object> fireOrderPaidWebhook(String authorization, JSONObject json, String body) {
    if (StringUtils.isEmpty(authorization)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter");
    }

    // 验证签名
    String serverSignature = sha1(body + xsollaChargeConfig.getSecretKey());
    String clientSignature = authorization.substring(10);
    if (!clientSignature.equals(serverSignature)) {
      log.error(
          LogAction.XSOLLA_CHARGE_PAY,
          "verifySignature",
          "clientAuthorization",
          clientSignature,
          "serverSignature",
          serverSignature,
          "body",
          body);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // 验证玩家是否存在 TODO external_id 直接是 PlayerId
    JSONObject userObject = json.getJSONObject("user");
    // String token = userObject != null ? userObject.getString("external_id") : "";
    // if (StringUtils.isEmpty(token)) {
    //   log.error(LogAction.XSOLLA_CHARGE_PAY, "token is empty");
    //   return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    // }
    //
    // AccountVo accountByToken = accountServer.getAccountByToken(token);
    // if (accountByToken == null) {
    //   log.error(LogAction.XSOLLA_CHARGE_PAY, "parse token failed");
    //   return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    // }
    // long playerId = accountByToken.getId();

    long playerId = userObject != null ? Long.parseLong(userObject.getString("external_id")) : 0L;
    AccountEntity accountEntity = accountDao.getAccountEntity(playerId);
    if (accountEntity == null) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "playerId not exist");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    JSONObject billing = json.getJSONObject("billing");
    if (billing == null) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "billing is null", "playerId", playerId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    JSONObject settings = billing.getJSONObject("settings");
    int clientProjectId = 0;
    int clientMerchantId = 0;
    if (settings != null) {
      clientProjectId = settings.getIntValue("project_id", 0);
      clientMerchantId = settings.getIntValue("merchant_id", 0);
    }

    if (clientProjectId != xsollaChargeConfig.getProjectId()) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "check projectId fail", "playerId", playerId,
          "clientProjectId", clientProjectId, "projectId", xsollaChargeConfig.getProjectId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    if (clientMerchantId != xsollaChargeConfig.getMerchantId()) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "check merchantId fail", "playerId", playerId,
          "clientMerchantId", clientMerchantId, "merchantId", xsollaChargeConfig.getMerchantId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    JSONObject orderObject = json.getJSONObject("order");
    if (orderObject == null) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "order is empty", "playerId", playerId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    int channelOrderId = orderObject.getIntValue("id");
    if (channelOrderId == 0) {
      log.error(LogAction.XSOLLA_CHARGE_PAY, "channelOrderId is null", "playerId", playerId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    // 付款模式。对于真实支付，使用default；对于测试性支付，使用sandbox。
    String mode = orderObject.getString("mode") == null ? "" : orderObject.getString("mode");
    // 支付货币类型。real-真实货币; unknown-免费订单; virtual-虚拟货币
    String currencyType =
        orderObject.getString("currency_type") == null
            ? ""
            : orderObject.getString("currency_type");
    // 购物车总价
    String amount = orderObject.getString("amount") == null ? "" : orderObject.getString("amount");
    // 发票id
    String invoiceId =
        orderObject.getString("invoice_id") == null ? "" : orderObject.getString("invoice_id");

    JSONArray items = json.getJSONArray("items");
    if (items == null || items.isEmpty()) {
      log.error(
          LogAction.XSOLLA_CHARGE_PAY,
          "items is empty",
          "playerId",
          playerId,
          "channelOrderId",
          channelOrderId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    List<XsollaGoodsVO> goodsDtoList = new ArrayList<>();
    for (Object item : items) {
      JSONObject itemObject = (JSONObject) item;
      String sku = itemObject.getString("sku");
      ConfigPayID configPayID = ShopConfigManager.getConfigPayIDByProductId(sku);
      if (configPayID == null) {
        log.error(
            LogAction.XSOLLA_CHARGE_PAY,
            "sku not exist",
            "playerId",
            playerId,
            "channelOrderId",
            channelOrderId,
            "sku",
            sku);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      int goodsId = configPayID.getGoodsId();

      // 数量
      String numStr = itemObject.getString("quantity");
      if (StringUtils.isEmpty(numStr)) {
        log.error(
            LogAction.XSOLLA_CHARGE_PAY,
            "num is null",
            "playerId",
            playerId,
            "channelOrderId",
            channelOrderId,
            "sku",
            sku);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      int num = Integer.parseInt(numStr);

      goodsDtoList.add(new XsollaGoodsVO(goodsId, sku, num, XSOLLA_UN_FINISH_STATE));
    }

    String goodsInfos = convert2GoodsInfo(goodsDtoList);
    // 记录信息
    xsollaChargeInfoDao.insertXsollaChargeInfo(
        new XsollaChargeInfo(
            channelOrderId,
            playerId,
            mode,
            currencyType,
            amount,
            invoiceId,
            XSOLLA_UN_FINISH_STATE,
            goodsInfos,
            new Date(),
            json.toString(),
            new Date()));

    log.info(
        LogAction.XSOLLA_CHARGE_PAY,
        "asyncPay",
        "playerId",
        playerId,
        "channelOrderId",
        channelOrderId,
        "goodsInfos",
        goodsInfos);

    // 异步发货
    asyncPay(playerId, channelOrderId, mode, goodsDtoList);

    return ResponseEntity.ok().build();
  }

  @Async
  public void asyncPay(
      long playerId, int channelOrderId, String mode, List<XsollaGoodsVO> goodsDtoList) {
    HttpResult httpResult = RestUtil.postForObject(
        ChargeConstants.WORLD_SERVER_URL + "/charge/chargeByXsolla",
        new HttpEntity<>(new XsollaPurchaseVO(playerId, channelOrderId, mode, goodsDtoList)),
        HttpResult.class);
    log.info(LogAction.XSOLLA_CHARGE_PAY, "asyncPayResult", httpResult);
  }


  /**
   * 生成签名
   *
   * @param input input
   * @return java.lang.String
   * @since 2025/6/11 20:34
   */
  public static String sha1(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder sb = new StringBuilder();
      for (byte b : hashInBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 algorithm is not available", e);
    }
  }

  /**
   * goodsInfos 转 第三方支付商品信息dto列表
   *
   * @param goodsInfos goodsInfos
   * @return java.util.List<com.game.charge.dto.XsollaGoodsDto>
   * @since 2025/6/12 16:10
   */
  private List<XsollaGoodsVO> convert2GoodsDto(String goodsInfos) {
    if (StringUtils.isBlank(goodsInfos)) {
      return Collections.emptyList();
    }

    List<XsollaGoodsVO> goodsDtos = new ArrayList<>();
    String[] split = goodsInfos.split(Symbol.SEMICOLON);
    for (String item : split) {
      String[] strings = item.split(Symbol.COLON);
      String sku = strings[0];
      ConfigPayID configPayID = ShopConfigManager.getConfigPayIDByProductId(sku);
      int goodsId = configPayID.getGoodsId();
      int num = Integer.parseInt(strings[1]);
      int state = Integer.parseInt(strings[2]);

      goodsDtos.add(new XsollaGoodsVO(goodsId, sku, num, state));
    }

    return goodsDtos;
  }

  /**
   * 第三方支付商品信息dto列表 转 goodsInfos
   *
   * @param goodsDtos goodsDtos
   * @return java.lang.String
   * @since 2025/6/12 16:10
   */
  private String convert2GoodsInfo(List<XsollaGoodsVO> goodsDtos) {
    // 购买商品信息: sku(支付ID):数量:状态(0-未发货;1-已发货);sku:数量:状态(0-未发货;1-已发货);
    StringBuilder sb = new StringBuilder();
    for (XsollaGoodsVO xsollaGoodsVO : goodsDtos) {
      sb.append(xsollaGoodsVO.getSku())
          .append(Symbol.COLON)
          .append(xsollaGoodsVO.getNum())
          .append(Symbol.COLON)
          .append(xsollaGoodsVO.getState())
          .append(Symbol.SEMICOLON);
    }
    return sb.toString();
  }

  /**
   * 确认发货回调
   *
   * @param xsollaPurchaseVO xsollaPurchaseVO
   * @return com.game.http.core.HttpResult
   * @since 2025/6/12 12:02
   */
  public HttpResult confirmPurchase(XsollaPurchaseVO xsollaPurchaseVO) {
    XsollaChargeInfo xsollaChargeInfo =
        xsollaChargeInfoDao.getXsollaChargeInfo(xsollaPurchaseVO.getChannelOrderId());
    List<XsollaGoodsVO> goodsDtoList = xsollaPurchaseVO.getGoodsDtoList();
    boolean allSuccess = goodsDtoList.stream().allMatch(o -> o.getState() == XSOLLA_FINISH_STATE);
    xsollaChargeInfo.setState(allSuccess ? XSOLLA_FINISH_STATE : XSOLLA_UN_FINISH_STATE);
    xsollaChargeInfo.setGoodsInfos(convert2GoodsInfo(goodsDtoList));
    xsollaChargeInfoDao.updateById(xsollaChargeInfo);

    log.info(
        LogAction.XSOLLA_CHARGE_PAY,
        "confirmPurchase",
        "playerId",
        xsollaPurchaseVO.getPlayerId(),
        "channelOrderId",
        xsollaChargeInfo.getId(),
        "goodsInfos",
        convert2GoodsInfo(goodsDtoList),
        "allSuccess",
        allSuccess);

    return HttpResult.success();
  }
}
