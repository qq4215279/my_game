/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.system.message;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.mumu.common.proto.message.core.ErrorCode;

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
    /** 消息大小 */
    private Integer messageSize;
    /** 协议id */
    private Integer messageId;
    /** 服务id */
    private Integer serviceId;
    /** 客户端发送时间 */
    private Long clientSendTime;
    /** 服务端发送时间 */
    private Long serverSendTime;
    /** seqId */
    private Integer clientSeqId;
    /** 版本号 */
    private Integer version;
    /** 错误码 */
    private ErrorCode errorCode;
    /** from服务器id */
    private Integer fromServerId;
    /** to服务器id */
    private Integer toServerId;
    /** 玩家id */
    private Long playerId;
    /** 消息类型 */
    private MessageTypeEnum messageType;

}
