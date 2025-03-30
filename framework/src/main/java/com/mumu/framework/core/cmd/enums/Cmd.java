/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd.enums;

import com.mumu.common.proto.message.gate.core.ConnectConfirmMsgCE;
import com.mumu.common.proto.message.gate.core.ConnectConfirmMsgEC;
import com.mumu.common.proto.message.gate.core.HeartbeatMsgCE;
import com.mumu.common.proto.message.gate.core.HeartbeatMsgEC;
import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.MessageTypeEnum;
import com.mumu.framework.core.cloud.ServiceType;

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
 * R - Register 注册服
 * I - Chat 好友聊天服
 * @author liuzhen
 * @version 1.0.0 2025/3/30 12:49
 */
public enum Cmd {
    /** 心跳消息 */
    HeartbeatMsg(ServiceType.GATE, HeartbeatMsgCE.class, HeartbeatMsgEC.class),
    /** 连接验证消息 */
    ConnectConfirmMsg(ServiceType.GATE, ConnectConfirmMsgCE.class, ConnectConfirmMsgEC.class)
    ;

    /** 消息所属服务id组 */
    @Getter
    private ServiceType serviceType;
    /** 请求协议消息结构体类型 */
    @Getter
    private Class<?> reqMsgClass;
    /** 响应协议消息结构体类型 */
    @Getter
    private Class<?> resMsgClass;

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
        header.setServiceId(serviceType.getServiceId());
        return header;
    }
}
