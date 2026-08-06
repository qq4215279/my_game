package com.mumu.game.proto.charge;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCGetOrderInfoMessage
 * 响应查询订单信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCGetOrderInfoMessage {
  /** 商品id */
  private Integer goodsId;
  /** 玩家渠道 */
  private String channel;
  /** 支付类型 */
  private String payType;
  /** 额外信息 */
  private String extraInfo;
  /** 购买商品 */
  private Integer extraGoodsId;
}