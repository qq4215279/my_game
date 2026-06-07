package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * OnPushFunctionStateListMessage
 * 推送功能状态变更消息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class OnPushFunctionStateListMessage {
  /**功能id(0为全部功能) */
  private Integer functionId;
  /** 功能列表(此功能以及所有子功能) */
  private java.util.List<SingleFunctionInfoBean> singleFunctionInfoBeanList = new java.util.ArrayList<>();
}