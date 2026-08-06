package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import com.mumu.game.proto.item.ItemBean;
import lombok.Data;


/**
 * WCBuyShopGoodsMessage
 * 响应购买商品
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCBuyShopGoodsMessage {
  /** 商品获得 */
  private Integer goodsId;
  /** 商品获得 */
  private java.util.List<ItemBean> rewards = new java.util.ArrayList<>();
  /** 道具不足弹窗礼包列表 */
  private java.util.List<GoodsBean> popGoodsList = new java.util.ArrayList<>();
  /** 冻结道具总数量 */
  private Long freezeItemNum;
  /** 弹窗礼包原因列表 */
  private java.util.List<PopGoodsReason> popGoodsReasonList = new java.util.ArrayList<>();
}