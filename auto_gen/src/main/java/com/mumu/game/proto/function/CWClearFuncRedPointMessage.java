package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWClearFuncRedPointMessage
 * 请求消除功能红点消息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWClearFuncRedPointMessage {
  /** 功能id */
  private Integer functionId;
  /** 参数0 */
  private int arg0;
  /** 参数1 */
  private String arg1;
}