package com.mumu.game.proto.charge;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWCreateOrderMessage
 * 请求创建订单
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWCreateOrderMessage {
  /** 商品id */
  private Integer goodsId;
  /** 支付类型 */
  private String payType;
  /** 额外信息 */
  private String extraInfo;
  /** 购买商品 */
  private Integer extraGoodsId;
}