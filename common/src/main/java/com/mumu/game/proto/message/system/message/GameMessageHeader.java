/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.proto.message.system.message;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.mumu.game.proto.message.core.ErrorCode;

import lombok.Data;

/**
 * GameMessageHeader
 * 游戏消息头信息
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:30
 */
@ProtobufClass
@Data
public class GameMessageHeader {
    /** 协议id */
    private Integer messageId;
    /** 消息类型 */
    private MessageTypeEnum messageType;
    /** 请求序号 */
    private Integer seq;
    /** 发送时间 */
    private Long sendTime;
    /** 版本号 */
    private Integer version;
    /** 玩家id */
    private Long playerId;
    /** 错误码 */
    private ErrorCode errorCode;
}
