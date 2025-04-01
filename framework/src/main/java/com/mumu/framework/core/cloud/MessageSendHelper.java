/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cloud;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.servlet.session.SessionManager;
import com.mumu.framework.util.JProtoBufUtil;

/**
 * MessageSendHelper
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 20:09
 */
public class MessageSendHelper {


    public void sendMessage(GameMessagePackage gameMessagePackage, GameChannelPromise promise) {
        long playerId = gameMessagePackage.getHeader().getPlayerId();
        int gateServerId = gameMessagePackage.getHeader().getToServerId();
        IoSession gateSession = SessionManager.self().getServerSession(ServiceType.GATE, gateServerId);
        if (gateSession == null) {
            LogTopic.NET.error("MessageSendHelper.sendMessage", "error", "gateSession is null", "gateServerId",
                    gateServerId, "playerId", playerId);
            return;
        }

        // 转protobuf
        byte[] data = JProtoBufUtil.encode(gameMessagePackage);
        // 发出消息
        gateSession.write(data);

        //
        promise.setSuccess();
    }
}
