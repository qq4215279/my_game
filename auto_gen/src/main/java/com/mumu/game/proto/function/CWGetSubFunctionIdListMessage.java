package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetSubFunctionIdListMessage
 * 请求获取子功能id列表消息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetSubFunctionIdListMessage {
  /** 功能id */
  private Integer functionId;
}