package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * GoodsBean
 * 商品信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class GoodsBean {
  /** 商品id */
  private Integer goodsId;
  /** 商品基本信息 */
  private CommonGoodsBean commonGoods;
  /** 拍脸礼包信息 */
  private PopFaceBean popFaceBean;
}