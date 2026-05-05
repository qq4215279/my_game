/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.listener;

import com.mumu.game.core.cmd.enums.ICmd;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.mumu.game.core.cmd.CmdDispatch;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.CmdManager;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.IoSession;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.net.session.SessionManager;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

import jakarta.annotation.Resource;

/**
 * GatewayHandlerListener
 * 网关的消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
@Component
@ConditionalOnExpression("#{T(com.mumu.game.core.net.consts.ServiceType).curr() == T(com.mumu.game.core.net.consts.ServiceType).GATEWAY}")
public class GatewayHandlerListener extends AbstractHandlerListener {

    @Resource
    SessionManager sessionManager;

    /** 消息管理类，负责管理根据消息id，获取对应的消息类实例 */
    @Resource
    private CmdDispatch cmdDispatch;

    @Override
    protected void doCommand(MessageContext context) {
        // 1. 收到client内部服务器请求，处理或转发给玩家
        if (context.getSession().hasAttr(NetConstants.SESSION_CLIENT)) {
            readInner(context);

            // 2. 玩家请求
        } else {
            readOut(context);
        }
    }

    // 收到内部服务器请求，处理或转发给玩家
    private void readInner(MessageContext context) {
        GameMessageHeader header = context.getMessagePackage().getHeader();
        // 1.1. rpc 请求或响应
        if (header.getMessageType() == MessageTypeEnum.RPC_REQUEST || header.getMessageType() == MessageTypeEnum.RPC_RESPONSE) {
            MessageSender.send(context);

            // 1.2. 返回给玩家客户端
        } else {
            Long playerId = context.getPlayerId();
            if (playerId == null) {
                LogTopic.NET.warn("innerRead", "playerId is null", "context", context);
                return;
            }
            // 转发给玩家
            IoSession outSession = sessionManager.getOutSession(playerId);
            if (outSession == null) {
                LogTopic.NET.warn("innerRead", "player session is null", "context", context);
                return;
            }

            MessageSender.send(outSession, context.getMessagePackage());
        }

    }

    // 接收外部玩家客户端消息处理
    private void readOut(MessageContext context) {
        GameMessageHeader header = context.getMessagePackage().getHeader();

        // 2.1. 非gateway 协议，转发给其他游戏服
        ICmd cmd = CmdManager.getCmd(header.getMessageId());
        if (cmd.getServiceType() != ServiceType.GATEWAY) {
            MessageSender.send(context);
            return;
        }

        // 2.2. Gateway 本服处理
        if (cmdDispatch.inSelf(context.getCmd())) {
            cmdDispatch.invokeMethod(context);
        } else {
            LogTopic.NET.warn("Not found command", "context", context);
        }
    }
}
