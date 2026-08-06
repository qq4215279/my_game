package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetShopGoodsByGoodsIdMessage
 * 请求商品信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetShopGoodsByGoodsIdMessage {
  /** 商品id */
  private Integer goodsId;
}