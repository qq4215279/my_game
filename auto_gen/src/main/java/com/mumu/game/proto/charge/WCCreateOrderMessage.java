package com.mumu.game.proto.charge;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCCreateOrderMessage
 * 响应创建订单
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCCreateOrderMessage {
  /** 订单id */
  private String orderId;
}