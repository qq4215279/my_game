package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCGetShopGoodsMessage
 * 响应商品列表信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCGetShopGoodsMessage {
  /** 功能id */
  private Integer functionId;
  /** 商品列表信息 */
  private java.util.List<GoodsBean> goodsList = new java.util.ArrayList<>();
}