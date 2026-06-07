package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * SingleFunctionInfoBean
 * 功能信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class SingleFunctionInfoBean {
  /** 功能id */
  private Integer functionId;
  /** 是否开放 */
  private Boolean hasOpen;
  /** 是否有红点 */
  private Boolean hasRedPoint;
  /** 开放的活动组件ID列表 */
  private java.util.List<Integer> activityIds = new java.util.ArrayList<>();
  /** 赛季开始时间，无默认-1 */
  private Long startTime;
  /** 赛季结束时间，无默认-1 */
  private Long endTime;
  /** 参数。注：只用于记录简单数据，复杂信息走协议！ */
  private String param;
  /** 功能配置基本信息 */
  private ConfigFunctionInfoBean configFunctionInfoBean;
}