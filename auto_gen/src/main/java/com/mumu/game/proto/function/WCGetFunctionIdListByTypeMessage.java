package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * WCGetFunctionIdListByTypeMessage
 * 响应获取功能列表By功能类型
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class WCGetFunctionIdListByTypeMessage {
  /** 活动功能信息bean列表 */
  private java.util.List<ActivityFunctionInfoBean> activityBannerBeanList = new java.util.ArrayList<>();
}