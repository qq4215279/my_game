package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetShopGoodsByTypeMessage
 * 请求商品列表信息ByType
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetShopGoodsByTypeMessage {
  /** 商品类型 */
  private Integer type;
}