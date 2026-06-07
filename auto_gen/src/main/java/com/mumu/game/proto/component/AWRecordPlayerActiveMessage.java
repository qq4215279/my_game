package com.mumu.game.proto.component;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * AWRecordPlayerActiveMessage
 * 请求记录玩家活跃信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class AWRecordPlayerActiveMessage {
  /** 类型 0: 在线时间; 1: 聊天次数; 2: 游戏对局; 3: 送礼次数 */
  private Integer type;
  /** 参数 */
  private String param;
}