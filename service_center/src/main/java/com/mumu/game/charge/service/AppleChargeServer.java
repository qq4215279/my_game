package com.mumu.game.charge.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.apple.itunes.storekit.client.APIException;
import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.client.GetTransactionHistoryVersion;
import com.apple.itunes.storekit.migration.ReceiptUtility;
import com.apple.itunes.storekit.model.AccountTenure;
import com.apple.itunes.storekit.model.ConsumptionRequest;
import com.apple.itunes.storekit.model.ConsumptionStatus;
import com.apple.itunes.storekit.model.DeliveryStatus;
import com.apple.itunes.storekit.model.HistoryResponse;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.LifetimeDollarsPurchased;
import com.apple.itunes.storekit.model.LifetimeDollarsRefunded;
import com.apple.itunes.storekit.model.Platform;
import com.apple.itunes.storekit.model.PlayTime;
import com.apple.itunes.storekit.model.RefundPreference;
import com.apple.itunes.storekit.model.TransactionHistoryRequest;
import com.apple.itunes.storekit.model.UserStatus;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.http.HttpCode;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogAction;
import com.mumu.game.log.LogTopic;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;

/**
 * AppleChargeServer 苹果充值Server
 * @author liuzhen
 * @version 1.0.0 2025/1/14 16:55
 */
@Service
public class AppleChargeServer {
  /**log */
  private final static LogTopic log = LogTopic.ACTION;

  @Resource
  private ChargeInfoDao chargeInfoDao;

  @Resource
  private AppStoreServerAPIClient prodClient;
  @Resource
  private AppStoreServerAPIClient sandboxClient;
  @Resource
  private SignedDataVerifier prodSignedDataVerifier;
  @Resource
  private SignedDataVerifier sandboxSignedDataVerifier;

  @Async
  public void asyncPay(long playerId, String transactionReceipt) {
    HttpResult pay = pay(playerId, transactionReceipt);
    log.info(LogAction.APPLE_CHARGE_PAY,"asyncPay-appleCallbackPay", "finish", "playerId", playerId, "HttpResult", pay);
  }

  /**
   * 苹果支付成功回调
   * @param playerId           playerId
   * @param transactionReceipt 苹果交易流水号
   * @return com.game.http.core.HttpResult
   * @since 2025/1/14 16:58
   */
  public HttpResult pay(long playerId, String transactionReceipt) {
    if (StringUtils.isEmpty(transactionReceipt)) {
      log.error(LogAction.APPLE_CHARGE_PAY,"playerId", "transactionReceipt", playerId, transactionReceipt);
      return HttpResult.error(HttpCode.FAIL, "参数为空");
    }

    // 1. 解析 transactionReceipt
    String transactionId = "";
    try {
      transactionId = new ReceiptUtility().extractTransactionIdFromAppReceipt(transactionReceipt);
    } catch (IOException e) {
      log.error(e, LogAction.APPLE_CHARGE_PAY,"transactionReceipt parse fai", "playerId", playerId);
      return HttpResult.error(HttpCode.FAIL, "解析transactionReceipt fail");
    }

    log.info(LogAction.APPLE_CHARGE_PAY,"playerId", playerId, "transactionId", transactionId);


    // 2. 查询交易记录
    // 只查找3天内的订单
    long startDate = DateUtil.beginOfDay(new Date()).getTime() - TimeUnit.DAYS.toMillis(2);
    Pair<Boolean, List<String>> pair = getSignTransactionList(playerId, transactionId, startDate, null);
    List<String> signedTransactions = pair.getValue();
    if (signedTransactions.isEmpty()) {
      return HttpResult.error(HttpCode.FAIL, "查询交易记录失败！");
    }

    // 是否为沙盒环境
    boolean sandbox = pair.getKey();

    // 3. 解析并发货
    // 根据productId分组map  <productId, JWSTransactionDecodedPayload>
    Map<String, JWSTransactionDecodedPayload> productIdPayloadMap = getStringJWSTransactionDecodedPayloadMap(signedTransactions, sandbox);

    List<ChargeInfo> initChargeInfoList = chargeInfoDao.getChargeInfoListByApple(playerId, ChargeConstants.INIT_CHARGE_STATE);
    for (ChargeInfo chargeInfo : initChargeInfoList) {
      String productId = chargeInfo.getProductId();
      JWSTransactionDecodedPayload jwsTransactionDecodedPayload = productIdPayloadMap.get(productId);
      if (jwsTransactionDecodedPayload == null) {
        continue;
      }

      String channelOrderId = jwsTransactionDecodedPayload.getTransactionId();
      // 4. 支付验证
      // 已发货
      ChargeInfo chargeInfoByChannelOrderId = chargeInfoDao.getChargeInfoByChannelOrderId(playerId, productId, channelOrderId);
      if (chargeInfoByChannelOrderId != null && (chargeInfoByChannelOrderId.getState() == ChargeConstants.INIT_CHARGE_FINISH
              || chargeInfoByChannelOrderId.getState() == ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH)) {
        log.error(LogAction.APPLE_CHARGE_PAY,"appleCallbackPay", "checkOrder fail", "has pay done!", "chargeInfo", chargeInfoByChannelOrderId);
        continue;
      }

      // 5. 游戏服发货
      // 更新为可发货
      chargeInfo.setState(ChargeConstants.INIT_CHARGE_SUCCESS);
      chargeInfo.setChannelOrderId(channelOrderId);
      chargeInfoDao.save(chargeInfo);

      try {
        if (doPay(playerId, chargeInfo.getOrderId(), channelOrderId, sandbox)) {
          log.info(LogAction.APPLE_CHARGE_PAY,"pay success",
              "playerId", playerId, "orderId", chargeInfo.getOrderId(), "channelOrderId", channelOrderId, "sandbox", sandbox);
        }
      } catch (Exception e) {
        log.error(e, LogAction.APPLE_CHARGE_PAY,"doPay exception",
            "playerId", playerId, "orderId", chargeInfo.getOrderId(), "channelOrderId", channelOrderId, "sandbox", sandbox);
      }

    }

    return HttpResult.success();
  }

  /**
   * 查询订单列表
   * @param playerId playerId
   * @param transactionId transactionId
   * @param startDate 查询开始时间，无则传null
   * @param endDate 查询截止时间，无则传null
   * @return cn.hutool.core.lang.Pair<java.lang.Boolean,java.util.List<java.lang.String>> key: 是否为沙河环境; value: 订单列表
   * @since 2025/6/6 14:29
   */
  private Pair<Boolean, List<String>> getSignTransactionList(long playerId, String transactionId, Long startDate, Long endDate) {
    // 是否为沙盒环境
    boolean sandbox = false;
    // 循环查询
    boolean hasMore = true;
    // 版本号
    String revision = null;

    List<String> signedTransactions = new ArrayList<>();
    TransactionHistoryRequest request = new TransactionHistoryRequest().startDate(startDate).endDate(endDate).sort(TransactionHistoryRequest.Order.ASCENDING);
    try {
      while (hasMore) {
        HistoryResponse historyResponse = prodClient.getTransactionHistory(transactionId, revision, request, GetTransactionHistoryVersion.V2);
        signedTransactions.addAll(historyResponse.getSignedTransactions());

        hasMore = historyResponse.getHasMore();
        revision = historyResponse.getRevision();
      }

      // 沙河环境交易记录查询
    } catch (APIException e) {
      sandbox = true;
      try {
        while (hasMore) {
          HistoryResponse historyResponse = sandboxClient.getTransactionHistory(transactionId, revision, request, GetTransactionHistoryVersion.V2);
          signedTransactions.addAll(historyResponse.getSignedTransactions());

          hasMore = historyResponse.getHasMore();
          revision = historyResponse.getRevision();
        }

      } catch (Exception sio) {
        log.error(sio, LogAction.APPLE_CHARGE_PAY,"select sandbox transaction fail#Exception", "playerId", playerId, "transactionId", transactionId);
      }
    } catch (Exception e) {
      log.error(e, LogAction.APPLE_CHARGE_PAY,"select prod transaction fail#Exception", "playerId", playerId, "transactionId", transactionId);
    }

    return Pair.of(sandbox, signedTransactions);
  }

  /**
   * 根据productId分组，获取最新的JWSTransactionDecodedPayload
   * @param signedTransactions signedTransactions
   * @param sandbox sandbox
   * @return java.util.Map<java.lang.String,JWSTransactionDecodedPayload>: key: productId, value: JWSTransactionDecodedPayload
   * @since 2025/3/6 16:03
   */
  private Map<String, JWSTransactionDecodedPayload> getStringJWSTransactionDecodedPayloadMap(
      List<String> signedTransactions, boolean sandbox) {
    List<JWSTransactionDecodedPayload> payloadList = new ArrayList<>();

    SignedDataVerifier signedDataVerifier = getSignedDataVerifier(sandbox);

    for (int i = signedTransactions.size() - 1; i >= 0; i--) {
      String signedTransaction = signedTransactions.get(i);
      JWSTransactionDecodedPayload jwsTransactionDecodedPayload = null;
      try {
        jwsTransactionDecodedPayload = signedDataVerifier.verifyAndDecodeTransaction(
            signedTransaction);
      } catch (VerificationException e) {
        throw new RuntimeException(e);
      }

      payloadList.add(jwsTransactionDecodedPayload);
    }

    return payloadList.stream().collect(
        Collectors.toMap(JWSTransactionDecodedPayload::getProductId, Function.identity(),
            BinaryOperator.maxBy(Comparator.comparing(JWSTransactionDecodedPayload::getPurchaseDate))));
  }

  /**
   * 获取苹果签名验证对象
   * @param sandbox sandbox
   * @return com.apple.itunes.storekit.verification.SignedDataVerifier
   * @since 2025/6/6 14:03
   */
  private SignedDataVerifier getSignedDataVerifier(boolean sandbox) {
    if (sandbox) {
      return this.sandboxSignedDataVerifier;
    } else {
      return this.prodSignedDataVerifier;
    }
  }

  /**
   * 请求游戏服发货
   * @param playerId playerId
   * @param orderId orderId
   * @param transactionId transactionId
   * @param sandbox sandbox
   * @return boolean
   * @since 2025/1/15 15:31
   */
  private boolean doPay(long playerId, String orderId, String transactionId, boolean sandbox) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("orderId", orderId);
    paramMap.put("transactionId", transactionId);
    paramMap.put("sandbox", sandbox);

    try {
      String result = HttpUtil.get(ChargeConstants.WORLD_SERVER_URL + "/charge/chargeByApple", paramMap);
      log.info(LogAction.APPLE_CHARGE_PAY,"checkOrder#getOrderInfo", "playerId", playerId, "result", result);

      JSONObject resJson = new JSONObject(result);
      int code = resJson.getInt("code");
      if (code != 0) {
        log.error(LogAction.APPLE_CHARGE_PAY,"checkOrder#getOrderInfo fail", "playerId", playerId, "code", code);
        return false;
      }

    } catch (Exception e) {
      log.error(e, LogAction.APPLE_CHARGE_PAY,"checkOrder#getOrderInfo fail#playerId", playerId);
      return false;
    }

    return true;
  }


  /**
   * 补单By通过查询历史记录
   * @param playerId playerId
   * @param transactionId transactionId
   * @param startDate startDate
   * @param endDate endDate
   * @return com.game.http.core.HttpResult
   * @since 2025/2/28 16:33
   */
  public HttpResult pay4History(long playerId, String transactionId, long startDate, long endDate) {
    if (StringUtils.isEmpty(transactionId)) {
      transactionId = chargeInfoDao.getLatestChannelOrderIdByPayChannel(playerId, "ios");

      if (StringUtils.isEmpty(transactionId)) {
        log.info(LogAction.APPLE_CHARGE_PAY,"pay4History", "transactionId is empty", "playerId", playerId);
        return HttpResult.error(HttpCode.FAIL, "transactionId is empty");
      }
    }

    log.info(LogAction.APPLE_CHARGE_PAY, "pay4History", "playerId", playerId, "transactionId", transactionId, "startDate", new Date(startDate),
        "endDate", new Date(endDate));

    // 2. 查询交易记录
    Pair<Boolean, List<String>> pair = getSignTransactionList(playerId, transactionId, startDate, endDate);
    List<String> signedTransactions = pair.getValue();
    if (signedTransactions.isEmpty()) {
      return HttpResult.error(HttpCode.FAIL, "查询交易记录失败！");
    }

    // 是否为沙盒环境
    boolean sandbox = pair.getKey();

    // 3. 解析 signedTransactions
    List<String> orderIds = new ArrayList<>();
    for (int i = signedTransactions.size() - 1; i >= 0; i--) {
      String signedTransaction = signedTransactions.get(i);
      String orderId = parseAndPaySignTransaction(playerId, signedTransaction, sandbox);
      if (StringUtils.isNotEmpty(orderId)) {
        orderIds.add(orderId);
      }
    }

    return HttpResult.success().add("orderIds", orderIds);
  }

  /**
   * 解析并处理signedTransaction
   * @param playerId playerId
   * @param signedTransaction signedTransaction
   * @param sandbox sandbox
   * @return java.lang.String
   * @since 2025/2/28 16:30
   */
  private String parseAndPaySignTransaction(long playerId, String signedTransaction, boolean sandbox) {
    String productId = "";
    String channelOrderId = "";
    try {
      SignedDataVerifier signedDataVerifier = getSignedDataVerifier(sandbox);
      JWSTransactionDecodedPayload jwsTransactionDecodedPayload = signedDataVerifier.verifyAndDecodeTransaction(
          signedTransaction);
      productId = jwsTransactionDecodedPayload.getProductId();
      channelOrderId = jwsTransactionDecodedPayload.getTransactionId();

    } catch (Exception e) {
      return "";
    }

    if (StringUtils.isEmpty(productId)) {
      return "";
    }

    // 4. 支付验证
    // 已发货
    ChargeInfo chargeInfoByChannelOrderId = chargeInfoDao.getChargeInfoByChannelOrderId(playerId, productId, channelOrderId);
    if (chargeInfoByChannelOrderId != null && (chargeInfoByChannelOrderId.getState() == ChargeConstants.INIT_CHARGE_FINISH
        || chargeInfoByChannelOrderId.getState() == ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH)) {
      return "";
    }

    // 未创建订单
    ChargeInfo chargeInfo = chargeInfoDao.getLatestChargeInfo(playerId, productId);
    if (chargeInfo == null) {
      log.warn(LogAction.APPLE_CHARGE_PAY,"parseAndPaySignTransaction", "checkOrder fail#order not exist", "playerId", playerId, "productId", productId);
      return "";
    }
    // 已发货 0: 初始化; 1: 充值成功; 2: 发货成功
    if (chargeInfo.getState() == ChargeConstants.INIT_CHARGE_FINISH) {
      log.warn(LogAction.APPLE_CHARGE_PAY,"parseAndPaySignTransaction", "checkOrder fail#has pay done!", "playerId", playerId, "orderId", chargeInfo.getOrderId());
      return "";
    }

    // 5. 游戏服发货
    // 更新为可发货
    chargeInfo.setState(ChargeConstants.INIT_CHARGE_SUCCESS);
    chargeInfo.setChannelOrderId(channelOrderId);
    chargeInfoDao.save(chargeInfo);

    try {
      if (doPay(playerId, chargeInfo.getOrderId(), channelOrderId, sandbox)) {
        log.info(LogAction.APPLE_CHARGE_PAY,"parseAndPaySignTransaction", "pay success", "playerId", playerId, "orderId",
            chargeInfo.getOrderId(), "channelOrderId", channelOrderId, "sandbox", sandbox);
      }
    } catch (Exception e) {
      log.error(e, LogAction.APPLE_CHARGE_PAY,"parseAndPaySignTransaction", "doPay exception",
          "playerId", playerId, "orderId", chargeInfo.getOrderId(), "channelOrderId", channelOrderId, "sandbox", sandbox);
    }

    return chargeInfo.getOrderId();
  }


  /**
   * 确认发货（游戏服发货成功回调，回调Order服务确认购买）
   * @param playerId playerId
   * @param transactionId transactionId
   * @param sandbox sandbox
   * @return com.game.http.core.HttpResult
   * @since 2025/1/15 15:44
   */
  public HttpResult confirmPurchase(String orderId, long playerId, String transactionId, boolean sandbox) {
    log.info(LogAction.APPLE_CHARGE_PAY, "confirmPurchase", "orderId", orderId, "playerId",
        playerId, "transactionId", transactionId, "sandbox", sandbox);

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null || chargeInfo.getState() != ChargeConstants.INIT_CHARGE_FINISH) {
      log.error(LogAction.APPLE_CHARGE_PAY, "confirmPurchase", "appleConfirmPurchase fail, 游戏服未发货", "orderId", orderId, "playerId", playerId);
      return HttpResult.error(HttpCode.FAIL, "游戏服未发货");
    }

    ConsumptionRequest consumptionRequest = new ConsumptionRequest()
        .customerConsented(true)
        .consumptionStatus(ConsumptionStatus.FULLY_CONSUMED)
        .platform(Platform.APPLE)
        .sampleContentProvided(false)
        .deliveryStatus(DeliveryStatus.DELIVERED_AND_WORKING_PROPERLY)
        .accountTenure(AccountTenure.UNDECLARED)
        .playTime(PlayTime.UNDECLARED)
        .lifetimeDollarsRefunded(LifetimeDollarsRefunded.UNDECLARED)
        .lifetimeDollarsPurchased(LifetimeDollarsPurchased.UNDECLARED)
        .userStatus(UserStatus.UNDECLARED)
        .refundPreference(RefundPreference.UNDECLARED);

    try {
      AppStoreServerAPIClient client = null;
      if (sandbox) {
        client = sandboxClient;
      } else {
        client = prodClient;
      }

      client.sendConsumptionData(transactionId, consumptionRequest);

      chargeInfo.setState(ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH);
      chargeInfoDao.save(chargeInfo);

    } catch (IOException e) {
      log.error(e, LogAction.APPLE_CHARGE_PAY,"confirmPurchase", "confirmPurchase fail#playerId", playerId);
    } catch (APIException e) {
      throw new RuntimeException(e);
    }

    return HttpResult.success();
  }

}
