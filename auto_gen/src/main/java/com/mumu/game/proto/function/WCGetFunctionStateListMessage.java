package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCGetFunctionStateListMessage
 * 响应获取功能状态信息列表
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCGetFunctionStateListMessage {
  /** 功能列表(此功能以及所有子功能) */
  private java.util.List<SingleFunctionInfoBean> singleFunctionInfoBeanList = new java.util.ArrayList<>();
}