package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * ActivityFunctionInfoBean
 * 活动功能信息bean
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class ActivityFunctionInfoBean {
  /** 功能id */
  private Integer functionId;
  /** 功能配置基本信息 */
  private ConfigFunctionInfoBean configFunctionInfoBean;
}