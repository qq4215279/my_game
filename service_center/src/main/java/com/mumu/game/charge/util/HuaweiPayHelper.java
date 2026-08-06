package com.mumu.game.charge.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

/**
 * HuaweiPayHelper 华为支付帮助类
 * @author liuzhen
 * @version 1.0.0 2024/11/26 14:40
 */
public class HuaweiPayHelper {

  /**
   * test checkSign
   * @param args args
   * @since 2024/11/28 10:45
   */
  public static void main(String[] args) {
    String publicKey = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAuWPodHywXbSenUHNWTF6/DOkkXB8NWtG4qbtligbm3ofdYc3THHBC3QhZ5zSV6OOsXfxj+aJW4jr3JnyURIpNrlQigObvKkKCqhxpUpQ183L5aEDSs81f6LEqIkfHwuL5BikffFIKxc9a2yOfJoKpwtC4PRBkoMOblkMBT4UtKQEWC8ohBwnvFvS57XQaoBTqhTTNfGf6x18IXTkfI+QQGbqCBHJzVTb91Rep3Td6Bhzw5gAikXlUeQn6e1CCajYMMTv4NiJwV7BWVZcSZQi1I9V5EwmkeJnEQbmhs1jdHHa5Uziwk2gj4sxAF8UxD1q7xrAoKAK/Y0x+t4b8xZBFv81nsdGXXm4mH5e1DtFAFLwchUHovcr3J6dx9++ONI1cziSA/eKrsimrKizGaHbklKieqSJL8zqkcp+5X5aQ2OSHZmiNcj0XFUAYIvAqgxVz4T1/sIEwrC2GGv16JezzdO6OnuuGxU1s1WxW20FsPqtBLNhiu1knpPgun7HPQBFAgMBAAE=";

    String inAppPurchaseData = "{\"autoRenewing\":false,\"orderId\":\"20241127172121760e5dbb9075.112249195\",\"packageName\":\"com.cxx.baloot\",\"applicationId\":112249195,\"applicationIdString\":\"112249195\",\"kind\":0,\"productId\":\"gold_100000\",\"productName\":\"金币100000\",\"purchaseTime\":1732699285000,\"purchaseTimeMillis\":1732699285000,\"purchaseState\":0,\"developerPayload\":\"1861701938894094336\",\"purchaseToken\":\"000001936cece6d6f8139b142b491c2ba525e5fe8a79a4d6f3d4177798d94e64ec55cb9e56148cb3x5341.5.112249195\",\"consumptionState\":0,\"confirmed\":0,\"purchaseType\":0,\"currency\":\"SAR\",\"price\":299,\"country\":\"SA\",\"payOrderId\":\"sandboxAd6999d170b1ad00b52126c2bf14bb95\",\"payType\":\"34\",\"sdkChannel\":\"1\"}";
    String inAppSignature = "iSpNvHAmQpoMJ9l638qSqx1btTI5/MQCUamH1gYl24UXsikq/zNLRN9S/exoqbqUUXd25OAYwAl2cvT4ubI8J+D5XPvdvW/Emg+4Lr5fe+E3jqEl1gk5OwQcDxt1w5WS0pHsYcdHlJ+ot11d59ApxoQTX9ZmXj5M8Af2cGtFMN0Uvu5FG6UJYjCvv7Ai+bN1X9RGjWA0qILtqXY/pUmOf1bqsBVDr80yLZcCTin592oHEGHIOYemjTXRSJ152vPQtg+7fGkLoGZHsuQE+bWsCWpDbOBjFFcXsSuMOLiPQqVBQb6p6hiZrHQMeDq9Xcm9rS1ma4gSe4HI36rs0h/NoX+C1R8RT5nu4C7jLgB1ie0yl5uCYMUF+teaMA4ogFx1zKrZiXkXTUPrjeMW8Ir2Mh7IYqBdxJCZBhLLwqTdwgjTJubqFAlVzseT6ieQmnwx8mafcRYY3FiAbwhEJRjo2anL4QC6AMtmReqgA5twV9DJh4EtlWFwr0cqMT0/3EdQ";
    String signatureAlgorithm = "SHA256WithRSA";

    System.out.println(checkSign(inAppPurchaseData, inAppSignature, publicKey, signatureAlgorithm));
  }

  /**
   * 校验签名信息
   * @param content            结果字符串
   * @param sign               签名字符串
   * @param publicKey          IAP公钥
   * @param signatureAlgorithm 签名算法字段，可从接口返回数据中获取，例如：OwnedPurchasesResult.getSignatureAlgorithm()
   * @return 是否校验通过
   */
  public static boolean checkSign(String content, String sign, String publicKey,
      String signatureAlgorithm) {
    if (sign == null) {
      return false;
    }
    if (publicKey == null) {
      return false;
    }

    // 当signatureAlgorithm为空时使用默认签名算法
    if (signatureAlgorithm == null || signatureAlgorithm.length() == 0) {
      signatureAlgorithm = "SHA256WithRSA";
      System.out.println("doCheck, algorithm: SHA256WithRSA");
    }
    try {
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
      // 生成"RSA"的KeyFactory对象
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      byte[] decodedKey = Base64.decodeBase64(publicKey);
      // 生成公钥
      PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
      java.security.Signature signature = null;
      // 根据SHA256WithRSA算法获取签名对象实例
      signature = java.security.Signature.getInstance(signatureAlgorithm);
      // 初始化验证签名的公钥
      signature.initVerify(pubKey);
      // 把原始报文更新到签名对象中
      signature.update(content.getBytes(StandardCharsets.UTF_8));
      // 将sign解码
      byte[] bsign = Base64.decodeBase64(sign);
      // 进行验签
      return signature.verify(bsign);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Gets App Level AccessToken.
   * @param TOKEN_URL TOKEN_URL
   * @param CLIENT_ID CLIENT_ID
   * @param CLIENT_SECRET CLIENT_SECRET
   * @return java.lang.String
   * @since 2024/11/28 10:55
   */
  public static String getAppAccessToken(String TOKEN_URL, String CLIENT_ID, String CLIENT_SECRET) throws Exception {
    // fetch accessToken
    String grantType = "client_credentials";
    String msgBody = MessageFormat.format("grant_type={0}&client_secret={1}&client_id={2}",
        grantType, URLEncoder.encode(CLIENT_SECRET, "UTF-8"), CLIENT_ID);

    // 请求头
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    // 请求参数
    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("grant_type", grantType);
    bodyMap.put("client_secret", URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8));
    bodyMap.put("client_id", CLIENT_ID);
    String msgBody2 = JSONObject.toJSONString(bodyMap);

    HttpResponse httpResponse = HttpRequest.post(TOKEN_URL)
        .headerMap(headers, true)
        .body(msgBody)
        .execute();
    String response = httpResponse.body();

    JSONObject obj = JSONObject.parseObject(response);
    return obj.getString("access_token");
  }

  /**
   * Build Authorization in Header
   * @param appAt appAt
   * @return headers
   */
  public static Map<String, String> buildAuthorization(String appAt) {
    String oriString = MessageFormat.format("APPAT:{0}", appAt);
    String authorization = MessageFormat.format("Basic {0}",
        Base64.encodeBase64String(oriString.getBytes(StandardCharsets.UTF_8)));
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", authorization);
    headers.put("Content-Type", "application/json; charset=UTF-8");
    return headers;
  }

}
