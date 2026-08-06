package com.mumu.game.proto.shop.goods;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * GoldReturnBean
 * 金币回购礼包
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class GoldReturnBean {
  /** 拍脸时间 ms */
  private Long popFaceTime;
  /** 持续时间 ms */
  private Long delayTime;
  /** 损失金币 */
  private Long lossGold;
  /** 已购买次数 */
  private Integer buyCount;
  /** 限制购买次数 */
  private Integer limitCount;
  /** 折扣 */
  private String discount;
}