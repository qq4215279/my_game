/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.server;

import com.mumu.common.proto.message.core.ErrorCode;
import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.proto.message.system.message.MessageTypeEnum;
import com.mumu.framework.core.cmd.enums.Cmd;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.constants.ServiceType;
import com.mumu.framework.core.mvc.session.SessionManager;
import com.mumu.framework.util.JProtoBufUtil;

/**
 * MessageSender
 * 消息发送类
 * @author liuzhen
 * @version 1.0.0 2025/4/4 18:02
 */
public class MessageSender {

    public static void sendMessage(GameMessagePackage gameMessagePackage, GameChannelPromise promise) {
        GameMessageHeader header = gameMessagePackage.getHeader();
        long playerId = header.getPlayerId();
        ServiceType toServiceType = ServiceType.getServiceType(header.getToServiceId());
        int toServerId = header.getToServerId();


        IoSession session = SessionManager.self().getServerSession(toServiceType, toServerId);
        if (session == null) {
            LogTopic.NET.error("MessageSendHelper.sendMessage", "error", "gateSession is null", "gateServerId",
                    toServerId, "playerId", playerId);
            return;
        }

        // 发出消息
        session.write(gameMessagePackage);

        if (promise != null) {
            promise.setSuccess();
        }
    }


    public static GameMessagePackage reqProxy(
            Cmd cmd, ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd.getReqMessageId(), MessageTypeEnum.REQUEST, toServiceType, toServerId, playerId, errorCode, data);
    }

    public static GameMessagePackage resProxy(
            Cmd cmd, ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd.getResMessageId(), MessageTypeEnum.RESPONSE, toServiceType, toServerId, playerId, errorCode,
                data);
    }

    /**
     * 创建 GameMessagePackage
     * @param messageId 协议id
     * @param messageType 消息类型
     * @param toServiceType to服务id组
     * @param toServerId to服务器id
     * @param playerId 玩家id
     * @param errorCode 错误码
     * @param data data
     * @return com.mumu.common.proto.message.system.message.GameMessagePackage
     */
    private static GameMessagePackage proxy(
            int messageId, MessageTypeEnum messageType,
            ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        GameMessagePackage gameMessagePackage = new GameMessagePackage();

        GameMessageHeader header = new GameMessageHeader();
        header.setMessageId(messageId);
        header.setMessageType(messageType);

        // TODO 设置当前服务器信息
        header.setFromServerId(1);
        header.setFromServerId(1);

        header.setToServiceId(toServiceType.getServiceId());
        header.setToServerId(toServerId);
        header.setPlayerId(playerId);
        header.setErrorCode(errorCode == null ? ErrorCode.SUCCESS : errorCode);

        // TODO
        header.setClientSeqId(1000000);
        // TODO 获取客户端发送时间
        long now = System.currentTimeMillis();
        header.setClientSendTime(now);
        header.setServerSendTime(now);
        // TODO 获取当前服务器版本号
        header.setVersion(1000);

        gameMessagePackage.setHeader(header);
        gameMessagePackage.setBody(JProtoBufUtil.encode(data));

        return gameMessagePackage;
    }
}
