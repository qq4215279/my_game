package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * CWGetFunctionIdListByTypeMessage
 * 请求获取功能列表By功能类型
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class CWGetFunctionIdListByTypeMessage {
  /** 功能类型 0: 活动列表 */
  private Integer type;
  /** 构建配置信息 */
  private boolean buildConfig;
}