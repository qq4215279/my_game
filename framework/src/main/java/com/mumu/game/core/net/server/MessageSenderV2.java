/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.server;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.ICmd;
import com.mumu.game.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.session.SessionManager;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.utils.JProtoBufUtil;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

/**
 * MessageSender
 * 消息发送类
 * @author liuzhen
 * @version 1.0.0 2025/4/4 18:02
 */
@Deprecated
public class MessageSenderV2 {

    @Deprecated
    public static void sendMessage(GameMessagePackage gameMessagePackage, GameChannelPromise promise) {
        GameMessageHeader header = gameMessagePackage.getHeader();
        long playerId = header.getPlayerId();
        // TODO
        // ServiceType toServiceType = ServiceType.getServiceType(header.getToServiceId());
        // int toServerId = header.getToServerId();

        ServiceType toServiceType = ServiceType.GAME;
        int toServerId = 0;


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

}
