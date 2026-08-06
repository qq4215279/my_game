package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWBuyShopGoodsMessage
 * 请求购买商品
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWBuyShopGoodsMessage {
  /** 商品id */
  private Integer goodsId;
}