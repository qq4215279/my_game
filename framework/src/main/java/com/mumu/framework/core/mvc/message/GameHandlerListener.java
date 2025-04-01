/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.proto.message.system.message.MessageTypeEnum;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.cloud.ServiceType;
import com.mumu.framework.core.cmd.CmdDispatch;

import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.servlet.session.SessionManager;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.Resource;

/**
 * GameHandlerListener
 *
 * @author liuzhen
 * @version 1.0.0 2025/4/1 22:32
 */
public class GameHandlerListener extends AbstractHandlerListener {

    @Resource
    CmdDispatch cmdDispatch;

    public GameHandlerListener() {
    }

    @Override
    public void handleRead(IoSession session, GameMessagePackage gameMessagePackage) {
      //
        GameMessageHeader header = gameMessagePackage.getHeader();
        int serviceId = header.getServiceId();
        long playerId = gameMessagePackage.getHeader().getPlayerId();

        IoSession session = SessionManager.self().getPlayerSession(playerId);
        if (session == null) {
            LogTopic.NET.warn("channelRead", "session is null", "channel", session.channel());
            return;
        }

        // 客户端请求
        ServiceType serviceType = CmdManager.getCmd(header.getMessageId()).getServiceType();
        if (header.getMessageType() == MessageTypeEnum.REQUEST) {
            // 本服
            if (serviceType == ServiceType.GATE) {
                gameMessageConsumerManager.fireReadGameMessage(gameMessagePackage);

                // 其他服
            } else {
                Promise<Integer> promise = new DefaultPromise<>(ctx.executor());
                playerServiceManager.selectServerId(playerId, serviceType.getServiceId(), promise).addListener((GenericFutureListener<Future<Integer>>) future -> {
                    if (future.isSuccess()) {
                        Integer toServerId = future.get();
                        gameMessagePackage.getHeader().setToServerId(toServerId);
                        gameMessagePackage.getHeader().setFromServerId(gatewayServerConfig.getServerId());
                        // gameMessagePackage.getHeader().getAttribute().setClientIp(clientIp);
                        gameMessagePackage.getHeader().setPlayerId(playerId);


                        // TODO BusinessServerManager ClientServer 获取 session 发送消息到 game服

                        log.info("发送到{}消息成功->{}", gameMessagePackage.getHeader());

                    } else {
                        log.error("消息发送失败", future.cause());
                    }
                });

            }

            // 响应
        } else {

        }
    }

}