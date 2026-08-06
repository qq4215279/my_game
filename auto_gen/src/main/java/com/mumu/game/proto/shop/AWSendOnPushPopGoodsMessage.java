package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * AWSendOnPushPopGoodsMessage
 * 请求发送推送弹窗礼包
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class AWSendOnPushPopGoodsMessage {
  /** 弹窗礼包id列表 */
  private java.util.List<Integer> goodsIdList = new java.util.ArrayList<>();
  /** 弹窗礼包id */
  private Long targetPlayerId;
  /** 冻结道具总数量 */
  private Long freezeItemNum;
  /** 弹窗礼包原因列表 */
  private java.util.List<PopGoodsReason> popGoodsReasonList = new java.util.ArrayList<>();
}