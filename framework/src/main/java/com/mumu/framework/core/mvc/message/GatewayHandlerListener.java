/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.framework.core.game_netty.context.GameMessageConsumerManager;
import com.mumu.framework.core.mvc.server.MessageSender;
import org.springframework.stereotype.Component;

import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.MessageTypeEnum;
import com.mumu.framework.core.mvc.server.MessageContext;
import com.mumu.framework.core.mvc.constants.ServiceType;
import com.mumu.framework.core.cmd.enums.Cmd;
import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.mvc.constants.NetConstants;
import com.mumu.framework.core.mvc.session.SessionManager;

import jakarta.annotation.Resource;

/**
 * GatewayHandlerListener
 * 网关的消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
@Component
public class GatewayHandlerListener extends AbstractHandlerListener {

    @Resource
    SessionManager sessionManager;

    @Resource
    private GameMessageConsumerManager gameMessageConsumerManager;

    @Override
    public void handleRead(MessageContext context) {

        // TODO 1. 收到client内部服务器请求，处理或转发给玩家
        if (context.getSession().hasAttr(NetConstants.SESSION_CLIENT)) {
            innerRead(context);

            // 2. 玩家请求
        } else {
            outRead(context);
        }
    }

    // 收到内部服务器请求，处理或转发给玩家
    private void innerRead(MessageContext context) {
        GameMessageHeader header = context.getProxy().getHeader();
        // 1. TODO rpc 请求或响应 => 转发到其他游戏服
        if (header.getMessageType() == MessageTypeEnum.RPC_REQUEST || header.getMessageType() == MessageTypeEnum.RPC_RESPONSE) {

            // 2. 返回给client玩家
        } else {
            // TODO
        }


    }

    // 接收外部玩家消息处理
    private void outRead(MessageContext context) {
        GameMessageHeader header = context.getProxy().getHeader();

        // 1. Gateway本服处理
        Cmd cmd = CmdManager.getCmd(header.getMessageId());
        if (cmd.getServiceType() == ServiceType.GATE) {
            gameMessageConsumerManager.fireReadGameMessage(context);

            // 2. 转发给其他游戏服
        } else {
            MessageSender.sendMessage(context.getProxy(), null);
        }

    }
}
