package com.mumu.game.proto.charge;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWChargeByFakeMessage
 * 请求假购
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWChargeByFakeMessage {
  /** 商品id */
  private String orderId;
}