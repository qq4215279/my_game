package com.mumu.game.charge.service;

import com.mumu.game.business.player.enums.ChannelEnum;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.proto.message.core.ErrorCode;

/**
 * ChargeService
 * 充值service
 * @author liuzhen
 * @version 1.0.0 2024/9/26 16:25
 */
public interface ChargeService {

  /**
   * 创建订单
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @param num 商品数量
   * @param state 订单状态
   * @param payType 支付类型
   * @param extraInfo extraInfo
   * @param extraGoodsId extraGoodsId
   * @param needCheckBuy 是否需要购买校验 第三方支付无需校验
   * @since 2024/9/26 17:54
   */
  ResponseResult createOrder(long playerId, int goodsId, int num, int state, String payType, String extraInfo, int extraGoodsId, boolean needCheckBuy);

  /**
   * 获取订单信息
   * @param playerId 玩家id
   * @param orderId 订单id
   * @since 2024/9/26 17:55
   */
  ResponseResult getOrderInfo(long playerId, String orderId);

  /**
   * 华为充值支付
   * @param playerId playerId
   * @param orderId  orderId
   * @param huaweiOrderId  huaweiOrderId
   * @param purchaseToken  purchaseToken
   * @param productId  productId
   * @param accessToken  accessToken
   * @since 2024/11/26 19:40
   */
  void chargeByHuawei(long playerId, String orderId, String huaweiOrderId, String purchaseToken,
      String productId, String accessToken);

  /**
   * 苹果充值支付
   * @param playerId playerId
   * @param orderId orderId
   * @param transactionId transactionId
   * @param sandbox sandbox
   * @since 2025/1/15 15:27
   */
  void chargeByApple(long playerId, String orderId, String transactionId, boolean sandbox);

  /**
   * 谷歌充值支付
   * @param playerId playerId
   * @param orderId orderId
   * @param productId productId
   * @param purchaseToken purchaseToken
   * @since 2025/2/13 13:55
   */
  void chargeByGoogle(long playerId, String orderId, String productId, String purchaseToken);

  /**
   * 艾克索拉（第三方支付）充值支付
   * @param playerId playerId
   * @param orderId orderId
   * @param channelOrderId channelOrderId 
   * @return com.game.proto.core.ErrorCode
   * @since 2025/6/12 14:31
   */
  ErrorCode chargeByXsolla(long playerId, String orderId, String channelOrderId);

  /**
   * 请求假购
   * @param playerId 玩家id
   * @param orderId 订单id
   * @since 2024/10/9 14:34
   */
  ResponseResult chargeByFake(long playerId, String orderId);

  /**
   * do充值
   * @param playerId 玩家id
   * @param orderId 订单id
   * @param chargeChannel 充值渠道
   * @since 2024/9/26 17:55
   */
  boolean doCharge(long playerId, String orderId, ChannelEnum chargeChannel);
}
