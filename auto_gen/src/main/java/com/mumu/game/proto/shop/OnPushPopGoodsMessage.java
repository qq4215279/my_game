package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * OnPushPopGoodsMessage
 * 推送弹窗礼包
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class OnPushPopGoodsMessage {
  /** 道具不足弹窗礼包 */
  private java.util.List<GoodsBean> popGoodsList = new java.util.ArrayList<>();
  /** 冻结道具总数量 */
  private Long freezeItemNum;
  /** 弹窗礼包原因列表 */
  private java.util.List<PopGoodsReason> popGoodsReasonList = new java.util.ArrayList<>();
  /** 额外参数 */
  private String param;
}