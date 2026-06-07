package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetFunctionStateListMessage
 * 请求获取功能状态信息列表
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetFunctionStateListMessage {
  /** 功能id(0为全部功能) */
  private Integer functionId;
}