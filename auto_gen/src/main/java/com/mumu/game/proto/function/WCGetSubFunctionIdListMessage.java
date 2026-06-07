package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCGetSubFunctionIdListMessage
 * 响应获取子功能id列表消息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCGetSubFunctionIdListMessage {
  /** 活动功能信息bean列表 */
  private java.util.List<ActivityFunctionInfoBean> activityBannerBeanList = new java.util.ArrayList<>();
}