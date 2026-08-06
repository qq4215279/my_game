package com.mumu.game.proto.charge;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetOrderInfoMessage
 * 请求查询订单信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetOrderInfoMessage {
  /** 订单id */
  private String orderId;
}