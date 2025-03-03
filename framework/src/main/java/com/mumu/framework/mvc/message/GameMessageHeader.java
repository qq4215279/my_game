package com.mumu.framework.mvc.message;

import lombok.Data;

/**
 * GameMessageHeader
 * 游戏消息头信息
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:30
 */
@Data
public class GameMessageHeader {
    /** 消息大小 */
    private int messageSize;
    /** 协议id */
    private int messageId;
    /** 服务id */
    private int serviceId;
    /** 客户端发送时间 */
    private long clientSendTime;
    /** 服务端发送时间 */
    private long serverSendTime;
    /** seqId */
    private int clientSeqId;
    /** 版本号 */
    private int version;
    /** from服务器id */
    private int fromServerId;
    /** to服务器id */
    private int toServerId;
    /** 玩家id */
    private long playerId;
    /** 消息类型 */
    private MessageTypeEnum messageType;

}
