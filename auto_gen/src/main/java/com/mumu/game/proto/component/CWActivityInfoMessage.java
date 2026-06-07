package com.mumu.game.proto.component;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWActivityInfoMessage
 * -------------------------- 活动组件请求 -------------------------- 活动消息请求
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWActivityInfoMessage {
  /** 功能id */
  private Integer funcId;
  /** 参数 排行榜功能传入(DAILY WEEK MONTH TOTAL) */
  private String param;
}