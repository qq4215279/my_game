package com.mumu.game.charge.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mumu.game.charge.config.GoogleChargeConfig;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.charge.util.RestUtil;
import com.mumu.game.dict.consts.DictConstants;
import com.mumu.game.dict.dao.DictDao;
import com.mumu.game.http.HttpCode;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogAction;
import com.mumu.game.log.LogTopic;
import com.mumu.game.redis.RedisUtil;
import com.mumu.game.redis.constants.RedisKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;

import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GoogleChargeServer
 * 谷歌充值Server
 * @author liuzhen
 * @version 1.0.0 2025/2/12 14:54
 */
@Service
public class GoogleChargeServer {
  /**log */
  private final static LogTopic log = LogTopic.ACTION;

  private static final String url = "https://accounts.google.com/o/oauth2/token";

  /** 谷歌查询订单url */
  private static final String GET_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{purchaseToken}?access_token={accessToken}";
  /** 谷歌确认发货url */
  private static final String CONSUME_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{purchaseToken}:consume";


  @Resource
  private ChargeInfoDao chargeInfoDao;
  @Resource
  private DictDao dictDao;
  @Resource
  private GoogleChargeConfig googleChargeConfig;

  /**
   * 谷歌支付成功回调
   * @param playerId playerId
   * @param productId productId
   * @param purchaseToken purchaseToken
   * @return com.game.http.core.HttpResult
   * @since 2025/2/12 14:57
   */
  public HttpResult pay(long playerId, String productId, String purchaseToken) {
    if (StringUtils.isEmpty(productId) || StringUtils.isEmpty(purchaseToken)) {
      log.error(LogAction.GOOGLE_CHARGE_PAY,"emptyParams", "playerId", playerId, "productId", productId, "purchaseToken", purchaseToken);
      return HttpResult.error(HttpCode.FAIL, "参数为空");
    }

    log.info(LogAction.GOOGLE_CHARGE_PAY,"params", "playerId", playerId, "productId", productId, "purchaseToken", purchaseToken);

    boolean paySuccess = false;

    // 订单的购买状态。0: 已购买; 1: 已取消; 2: 待定
    int purchaseState = 2;
    try {
      // 获取订单信息
      String accessToken = RedisUtil.get(RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey());
      if (StringUtils.isEmpty(accessToken)) {
        // 刷新 Google访问令牌
        refreshGoogleAccessToken();
        accessToken = RedisUtil.get(RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey());
      }
      log.info(LogAction.GOOGLE_CHARGE_PAY,"tokenInfo", "playerId", playerId, "productId", productId, "purchaseToken", purchaseToken, "accessToken", accessToken, "packageName", googleChargeConfig.getPackageName());

      JSONObject responseObject = RestUtil.getForObject(GET_URL, JSONObject.class,
          googleChargeConfig.getPackageName(), productId, purchaseToken, accessToken);
      if (responseObject == null) {
        return HttpResult.error(HttpCode.FAIL, "response is null");
      }
      // 发送 GET 请求并接收响应
      /*String url = StrUtil.format(GET_URL, googleChargeConfig.getPackageName(), productId, purchaseToken, accessToken);
      log.info("googlePay#getProductInfo#url#{}", url);
      String accessTokenUrl = "?access_token=" + accessToken;

      HttpResponse httpResponse = HttpRequest.get(url + accessTokenUrl)
          // TODO 去除代理
          .setHttpProxy("127.0.0.1", 8118)
          .execute();
      String response = httpResponse.body();
      log.info("googlePay#getProductInfo#response#{}", response);

      com.alibaba.fastjson.JSONObject responseObject = JSON.parseObject(response);*/

      // 订单的购买状态。0: 已购买; 1: 已取消; 2: 待定
      purchaseState = responseObject.getIntValue("purchaseState");

      if (purchaseState == 0) {
        // 产品的消费状态。0: 尚未消耗; 1: 已消耗
        int consumptionState = responseObject.getIntValue("consumptionState");
        // Google服务商订单ID
        String channelOrderId = responseObject.getString("orderId");
        // 透传参数: 游戏订单id
        String orderId = responseObject.getString("obfuscatedExternalProfileId");
        if (consumptionState == 1) {
          log.error(LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#hasConsume",
              "playerId", playerId, "orderId", orderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }
        // 透传参数: 玩家id
        String playerIdStr = responseObject.getString("obfuscatedExternalAccountId");
        if (playerId != Integer.parseInt(playerIdStr)) {
          log.error(LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#playerIdStr not same",
              "playerId", playerId, "playerIdStr", playerIdStr, "orderId", orderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }

        // 已发货
        ChargeInfo chargeInfoByChannelOrderId = chargeInfoDao.getChargeInfoByChannelOrderId(
            playerId, productId, channelOrderId);
        if (chargeInfoByChannelOrderId != null
            && chargeInfoByChannelOrderId.getState() == ChargeConstants.INIT_CHARGE_FINISH) {
          log.error(
              LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#has pay done!",
              "playerId", playerId, "productId", productId, "channelOrderId", channelOrderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }

        ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
        if (chargeInfo == null) {
          log.error(
              LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#order not exist",
              "playerId", playerId, "productId", productId, "channelOrderId", channelOrderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }
        if (chargeInfo.getPlayerId() != playerId) {
          log.error(
              LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#playeId not same",
              "playerId", playerId, "chargeInfoPlayerId", chargeInfo.getPlayerId(), "productId", productId, "channelOrderId", channelOrderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }
        if (!chargeInfo.getProductId().equals(productId)) {
          log.error(
              LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#productId not same",
              "playerId", playerId, "productId", productId, "chargeInfoProductId", chargeInfo.getProductId(), "channelOrderId", channelOrderId);
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }

        // 已发货 0: 初始化; 1: 充值成功; 2: 发货成功
        if (chargeInfo.getState() == ChargeConstants.INIT_CHARGE_FINISH) {
          log.error(LogAction.GOOGLE_CHARGE_PAY,"checkOrder fail#has pay done!",
              "playerId", playerId, "orderId", chargeInfo.getOrderId());
          return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
        }

        // 更新为可发货
        chargeInfo.setState(ChargeConstants.INIT_CHARGE_SUCCESS);
        chargeInfo.setChannelOrderId(channelOrderId);
        chargeInfoDao.save(chargeInfo);

        try {
          if (doPay(playerId, orderId, purchaseToken)) {
            paySuccess = true;
            log.info(LogAction.GOOGLE_CHARGE_PAY,"pay success", "playerId", playerId, "orderId", orderId, "purchaseToken", purchaseToken);
          }
        } catch (Exception e) {
          log.error(e, LogAction.GOOGLE_CHARGE_PAY,"doPay exception#playerId#{}", playerId);
        }

      } else {
        log.error(LogAction.GOOGLE_CHARGE_PAY,"getOrderInfo fail", "playerId", playerId, "purchaseState", purchaseState);
      }

    } catch (Exception e) {
      log.error(e, LogAction.GOOGLE_CHARGE_PAY,"getOrderInfo exception#playerId", playerId);
    }

    if (!paySuccess) {
      log.error("appleCallbackPay#payFail#playerId", playerId);
      return HttpResult.error(HttpCode.FAIL, "callbackPay payFail");
    }

    return HttpResult.success();
  }

  @Deprecated
  private AndroidPublisher getAndroidPublisher() {
    try {
      String accessToken = RedisUtil.get(RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey());
      if (StringUtils.isEmpty(accessToken)) {
        return null;
      }

      // TODO 获取http对象
      HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

      ResourceLoader resourceLoader = new DefaultResourceLoader();
      org.springframework.core.io.Resource resource = resourceLoader.getResource("certs/google/halabaloot-fe0bf64a2e14.p12");

      PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
          SecurityUtils.getPkcs12KeyStore(),
          // new FileInputStream(new File("certs/google/halabaloot-fe0bf64a2e14.p12")),
          resource.getInputStream(),
          "notasecret", "privateKey", "notasecret");

      GoogleCredential credential = new GoogleCredential.Builder()
          .setTransport(transport)
          .setJsonFactory(JacksonFactory.getDefaultInstance())
          .setServiceAccountId(googleChargeConfig.getAccountId())
          .setServiceAccountScopes(AndroidPublisherScopes.all())
          .setServiceAccountPrivateKey(privateKey).build();

      return new AndroidPublisher.Builder(transport,
          JacksonFactory.getDefaultInstance(), credential.setAccessToken(accessToken)).build();
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * 请求游戏服发货
   * @param playerId playerId
   * @param orderId orderId
   * @param purchaseToken purchaseToken
   * @return boolean
   * @since 2025/2/13 15:21
   */
  private boolean doPay(long playerId, String orderId, String purchaseToken) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("orderId", orderId);
    paramMap.put("purchaseToken", purchaseToken);

    try {
      String result = HttpUtil.get(ChargeConstants.WORLD_SERVER_URL + "/charge/chargeByGoogle", paramMap);
      log.info(LogAction.GOOGLE_CHARGE_PAY,"checkOrder#getOrderInfo", "playerId", playerId, "result", result);

      JSONObject resJson = JSONObject.parseObject(result);
      int code = resJson.getIntValue("code");
      if (code != 0) {
        log.error(LogAction.GOOGLE_CHARGE_PAY,"checkOrder#getOrderInfo fail", "playerId", playerId, "code", code);
        return false;
      }

    } catch (Exception e) {
      log.error(e, LogAction.GOOGLE_CHARGE_PAY,"checkOrder#getOrderInfo fail#playerId", "playerId", playerId);
      return false;
    }

    return true;
  }

  /**
   * 请求获取refreshToken
   * @param code code
   * @return com.game.http.core.HttpResult
   * @since 2025/2/13 18:26
   */
  public HttpResult getGoogleRefreshToken(String code) {
    if (StringUtils.isEmpty(code)) {
      code = "4%2F0ASVgi3Kd7V8SHezVywYVG6RTKm2cqygqPMw-8D0WrMvifNHQAc9HwClJN9_Q85Oeje1MKg";
    }

    try {
      // HTTP 获取access_token
     /* Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
      headers.put("Accept", "text/plain;charset=utf-8");

      Map<String, String> bodyMap = new HashMap<>();
      bodyMap.put("grant_type", "authorization_code");
      bodyMap.put("code", code);
      bodyMap.put("client_id", googleChargeConfig.getClientId());
      bodyMap.put("client_secret", googleChargeConfig.getClientSecret());
      bodyMap.put("redirect_uri", googleChargeConfig.getRedirectUri());

      String response = null;
      try {
        HttpResponse httpResponse = HttpRequest.post(url)
            // TODO 去除代理
            .setHttpProxy("127.0.0.1", 8118)
            .headerMap(headers, true)
            .body(JSONUtil.toJsonStr(bodyMap))
            .execute();
        response = httpResponse.body();
      } catch (HttpException e) {
        log.error("callbackPay#checkToken fail#remote verify fail#");
      }

      log.info("getGoogleAccessToken#response#{},", response);
      com.alibaba.fastjson.JSONObject tokenObject = JSON.parseObject(response);*/


      // HttpHeaders headers = new HttpHeaders();
      // headers.setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.UTF_8));
      // headers.setAccept(Collections.singletonList(new MediaType("text", "plain", StandardCharsets.UTF_8)));

      Map<String, String> bodyMap = new HashMap<>();
      bodyMap.put("grant_type", "authorization_code");
      bodyMap.put("code", code);
      bodyMap.put("client_id", googleChargeConfig.getClientId());
      bodyMap.put("client_secret", googleChargeConfig.getClientSecret());
      bodyMap.put("redirect_uri", googleChargeConfig.getRedirectUri());

      JSONObject tokenObject = RestUtil.postForObject(url, new HttpEntity<>(bodyMap), JSONObject.class);
      if (tokenObject == null) {
        return HttpResult.error();
      }
      log.info(LogAction.GOOGLE_CHARGE_PAY,"getGoogleAccessToken#response", tokenObject);

      String accessToken = tokenObject.getString("access_token");
      String tokenType = tokenObject.getString("token_type");
      Long expiresIn = tokenObject.getLong("expires_in");
      String refreshToken = tokenObject.getString("refresh_token");

      // 记录
      dictDao.insertDict(DictConstants.GOOGLE_CODE, code, "google_code");
      dictDao.insertDict(DictConstants.GOOGLE_REFRESH_TOKEN, refreshToken, "googleRefreshToken");
      // 设置accessToken
      RedisUtil.set(RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey(), accessToken, expiresIn);
      log.info(LogAction.GOOGLE_CHARGE_PAY,"getGoogleAccessToken#getSuccess", "accessToken", accessToken, "expiresIn", expiresIn);

      Map<String, Object> resultMap = new HashMap<>();
      resultMap.put("access_token", accessToken);
      resultMap.put("token_type", tokenType);
      resultMap.put("expires_in", expiresIn);
      resultMap.put("refresh_token", refreshToken);
      return HttpResult.success(resultMap);
    } catch (Exception e) {
      log.error(e, LogAction.GOOGLE_CHARGE_PAY,"获取googleAccessToken失败");
      return HttpResult.error();
    }

  }

  /**
   * 重定向
   * @param request request
   * @param code code
   * @return com.game.http.core.HttpResult
   * @since 2025/2/14 15:42
   */
  public HttpResult redirect(HttpServletRequest request, String code) {
    Map<String, String> params = parseResponseReferToMap(request);
    // 浏览器将被重定向到带有code 参数的重定向URI，该参数看起来类似于4/eWdxD7b-YSQ5CNNb-c2iI83KQx19.wp6198ti5Zc7dJ3UXOl0T3aRLxQmbwI
    String parseCode = params.get("code");

    log.info(LogAction.GOOGLE_CHARGE_PAY,"googleCharge.redirect", "code", code, "parseCode", parseCode);

    return HttpResult.success().add("code", parseCode);
  }

  private Map<String, String> parseResponseReferToMap(HttpServletRequest request) {
    Map<String, String> params = new HashMap<>();
    Map requestParams = request.getParameterMap();
    for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
      String name = (String) iter.next();
      String[] values = (String[]) requestParams.get(name);
      String valueStr = "";
      for (int i = 0; i < values.length; i++) {
        valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
      }
      // 乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
      // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
      params.put(name, valueStr);
    }
    return params;
  }

  /**
   * 确认发货（游戏服发货成功回调，回调Order服务确认购买）
   * @param orderId      orderId
   * @param playerId      playerId
   * @param productId     productId
   * @param purchaseToken purchaseToken
   * @return com.game.http.core.HttpResult
   * @since 2025/2/12 17:04
   */
  public HttpResult confirmPurchase(String orderId, long playerId, String productId, String purchaseToken) {
    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null || chargeInfo.getState() != ChargeConstants.INIT_CHARGE_FINISH) {
      log.error(LogAction.GOOGLE_CHARGE_PAY,"googleConfirmPurchase fail, 游戏服未发货", "orderId", orderId, "playerId", playerId);
      return HttpResult.error(HttpCode.FAIL, "游戏服未发货");
    }

    log.info(LogAction.GOOGLE_CHARGE_PAY,"googleConfirmPurchase", "playerId", playerId, "productId", productId, "purchaseToken", purchaseToken);

    String accessToken = RedisUtil.get(RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey());
    if (StringUtils.isEmpty(accessToken)) {
      log.error(LogAction.GOOGLE_CHARGE_PAY,"confirmPurchase#google_charge_access_token is null");
      return HttpResult.error("confirmPurchase#google_charge_access_token is null");
    }

    // 设置请求头
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(accessToken);
    ResponseEntity<String> response = RestUtil.exchange(CONSUME_URL, HttpMethod.POST,
        new HttpEntity<>(headers), String.class
        , googleChargeConfig.getPackageName(), productId, purchaseToken);

    boolean isSuccesss = response != null && response.getStatusCode().is2xxSuccessful();
    log.info(LogAction.GOOGLE_CHARGE_PAY,"confirmPurchase", "successs", isSuccesss, "response", response);

    if (isSuccesss) {
      chargeInfo.setState(ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH);
      chargeInfoDao.save(chargeInfo);
    }

    /*// 发送 GET 请求并接收响应
    String url = StrUtil.format(CONSUME_URL, googleChargeConfig.getPackageName(), productId, purchaseToken);
    log.info("confirmPurchase#url#{}#accessToken#{}", url, accessToken);

    RestTemplate restTemplate = createRestTemplateWithProxy();
    // 设置请求头
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(accessToken);
    // 创建HttpEntity
    HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
    String body = "";
    try {
      // 发送POST请求
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
      body = response.getBody();
      log.info("confirmPurchase#isSuccess#{}#response#{}", body == null, body);
    } catch (RestClientException e) {
      log.error("confirmPurchase#fail");
    }*/

    return HttpResult.success();
  }

  private static RestTemplate createRestTemplateWithProxy() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    // TODO 去除代理
    factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)));
    // 创建RestTemplate实例
    RestTemplate restTemplate =  new RestTemplate(factory);

    return restTemplate;
  }

  /**
   * 刷新 Google访问令牌,保证其有效
   * @since 2025/2/13 19:21
   */
  @Scheduled(cron = "0 0/10 * * * ? ")
  public void refreshGoogleAccessToken() {
    // 设置accessToken
    String key = RedisKey.GOOGLE_CHARGE_ACCESS_TOKEN.buildKey();
    long expire = RedisUtil.getExpire(key);
    // 永久有效
    if (expire == -1) {
      log.error(LogAction.GOOGLE_CHARGE_PAY,"refreshGoogleAccessToken fail#forever valid", "expire", expire);
      return;
    }

    // 超过15分钟，不刷新
    if (expire > TimeUnit.MINUTES.toSeconds(15)) {
      log.info(LogAction.GOOGLE_CHARGE_PAY,"refreshGoogleAccessToken#notNeedRefresh#expire ge 300s#expire", expire);
      return;
    }

    String refreshToken = dictDao.getValue(DictConstants.GOOGLE_REFRESH_TOKEN);
    // 1.获取Google刷新令牌
    // HTTP Google服务器 刷新access_token
    String refreshTokenResponse = refreshGoogleAccessTokenResponse(refreshToken);
    if (StringUtils.isEmpty(refreshTokenResponse)) {
      log.error(LogAction.GOOGLE_CHARGE_PAY,"refreshGoogleAccessToken#refreshGoogleAccessTokenResponse fail#refreshTokenResponse", refreshTokenResponse);
      return;
    }

    JSONObject tokenObject = JSON.parseObject(refreshTokenResponse);
    String accessToken = tokenObject.getString("access_token");
    Long expiresIn = tokenObject.getLong("expires_in");
    // 设置
    RedisUtil.set(key, accessToken, expiresIn);
    log.info(LogAction.GOOGLE_CHARGE_PAY,"refreshGoogleAccessToken#refreshSuccess", "accessToken", accessToken, "expiresIn", expiresIn);
  }

  /**
   * 刷新accessToken
   * @param refreshToken refreshToken
   * @return java.lang.String
   * @since 2025/2/17 17:26
   */
  private String refreshGoogleAccessTokenResponse(String refreshToken) {
    /*String response = "";
    try {
      Map<String, String> headers = new HashMap<>();

      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
      headers.put("Accept", "text/plain;charset=utf-8");

      Map<String, String> bodyMap = new HashMap<>();
      bodyMap.put("grant_type", "refresh_token");
      bodyMap.put("client_id", googleChargeConfig.getClientId());
      bodyMap.put("client_secret", googleChargeConfig.getClientSecret());
      bodyMap.put("refresh_token", refreshToken);
      bodyMap.put("redirect_uri", googleChargeConfig.getRedirectUri());

      HttpResponse httpResponse = HttpRequest.post(url)
          // TODO 去除代理
          .setHttpProxy("127.0.0.1", 8118)
          .headerMap(headers, true)
          .body(JSONUtil.toJsonStr(bodyMap))
          .execute();
      response = httpResponse.body();

    } catch (HttpException e) {
      log.error("refreshGoogleAccessTokenResponse#fail", e);
    }*/

    // HttpHeaders headers = new HttpHeaders();
    // headers.setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.UTF_8));
    // headers.setAccept(Collections.singletonList(new MediaType("text", "plain", StandardCharsets.UTF_8)));

    Map<String, String> bodyMap = new HashMap<>();
    bodyMap.put("grant_type", "refresh_token");
    bodyMap.put("client_id", googleChargeConfig.getClientId());
    bodyMap.put("client_secret", googleChargeConfig.getClientSecret());
    bodyMap.put("refresh_token", refreshToken);
    bodyMap.put("redirect_uri", googleChargeConfig.getRedirectUri());

    return RestUtil.postForObject(url, new HttpEntity<>(bodyMap), String.class);
  }

}


