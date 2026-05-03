/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.enums;

import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.proto.message.gate.core.ConnectConfirmMsgCE;
import com.mumu.game.proto.message.gate.core.ConnectConfirmMsgEC;
import com.mumu.game.proto.message.gate.core.HeartbeatMsgCE;
import com.mumu.game.proto.message.gate.core.HeartbeatMsgEC;
import com.mumu.game.proto.message.server.ReconnectServerMsgAE;
import com.mumu.game.proto.message.server.ReconnectServerMsgEA;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

import lombok.Getter;

/**
 * Cmd 协议定义规范
 * 协议命名以收发方服务为前缀，如：CWEnterGame 表示 客户端 -> 大厅服的进入游戏请求
 * 前缀简写含义
 * C - Client 客户端
 * E - External 对外服，网关服
 * A - Any 任意服
 * W - World 大厅服
 * G - Game 游戏服
 * I - Chat 好友聊天服
 * Z - Center 中心服
 * @author liuzhen
 * @version 1.0.0 2025/3/30 12:49
 */
public enum Cmd {
    /** 心跳消息 */
    HeartbeatMsg(ServiceType.GATEWAY, HeartbeatMsgCE.class, HeartbeatMsgEC.class),
    /** 连接验证消息 */
    ConnectConfirmMsg(ServiceType.GATEWAY, ConnectConfirmMsgCE.class, ConnectConfirmMsgEC.class),
    /** 连接服务器 */
    ReconnectServerMsg(ServiceType.ALL, ReconnectServerMsgEA.class, ReconnectServerMsgAE.class)

    ;

    /** 消息所属服务id组 */
    @Getter
    private final ServiceType serviceType;
    /** 请求协议消息结构体类型 */
    @Getter
    private final Class<?> reqMsgClass;
    /** 响应协议消息结构体类型 */
    @Getter
    private final Class<?> resMsgClass;

    Cmd(ServiceType serviceType, Class<?> reqMsgClass, Class<?> resMsgClass) {
        this.serviceType = serviceType;
        this.reqMsgClass = reqMsgClass;
        this.resMsgClass = resMsgClass;
    }

    /**
     * 获取请求Cmd对应messageId
     * @return int
     */
    public int getReqMessageId() {
        return CmdManager.getReqMessageId(this);
    }

    /**
     * 获取相应Cmd对应messageId
     * @return int
     */
    public int getResMessageId() {
        return CmdManager.getResMessageId(this);
    }

    public GameMessageHeader buildGameMessageHeader(boolean req) {
        GameMessageHeader header = new GameMessageHeader();

        header.setMessageId(req ? CmdManager.getReqMessageId(this) : CmdManager.getResMessageId(this));
        header.setMessageType(req ? MessageTypeEnum.REQUEST : MessageTypeEnum.RESPONSE);
        return header;
    }
}
