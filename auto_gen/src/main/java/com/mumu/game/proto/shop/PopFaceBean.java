package com.mumu.game.proto.shop;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * PopFaceBean
 * 拍脸礼包
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class PopFaceBean {
  /** 拍脸时间 */
  private Long popFaceTime;
  /** 持续时间(毫秒值) */
  private Long delayTime;
  /** 是否有红点 */
  private Boolean hasPoint;
}