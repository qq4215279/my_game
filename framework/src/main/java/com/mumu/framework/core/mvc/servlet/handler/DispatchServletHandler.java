/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.cloud.PlayerServiceManager;
import com.mumu.framework.core.game_netty.context.GameMessageConsumerManager;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.message.MessageHandlerListener;
import com.mumu.framework.core.mvc.servlet.session.SessionManager;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * DispatchServletHandler
 * 消息分发处理器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:39
 */
public class DispatchServletHandler extends ChannelInboundHandlerAdapter {
    private static final LogTopic log = LogTopic.NET;

    /** 消息处理监听器 */
    private final MessageHandlerListener listener;

    private final PlayerServiceManager playerServiceManager;

    /**  */
    private final GameMessageConsumerManager gameMessageConsumerManager;

    /** 注入游戏网关服务配置信息。 */
    private GatewayServerConfig gatewayServerConfig;

    public DispatchServletHandler(MessageHandlerListener messageHandlerListener) {
        this.listener = messageHandlerListener;
        this.playerServiceManager = SpringContextUtils.getBean(PlayerServiceManager.class);
        this.gameMessageConsumerManager = SpringContextUtils.getBean(GameMessageConsumerManager.class);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (SessionManager.self().contains(channel.id().toString())) {
            LogTopic.NET.warn("channelActive", "session id duplicate", "channel", channel);
            return;
        }
        IoSession session = IoSession.of(channel);
        try {
            SessionManager.self().add(session);
            // 调用监听器
            listener.handleActive(session);
        } catch (Exception e) {
            LogTopic.NET.error(e, "channelActive", "channel", channel);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        IoSession session = SessionManager.self().get(channel.id().toString());
        if (session == null) {
            LogTopic.NET.warn("channelRead", "session is null", "channel", channel);
            return;
        }
        try {
            listener.handleRead(session, (GameMessagePackage) msg);
        } catch (Exception e) {
            LogTopic.NET.error(e, "channelRead", "channel", channel, "msg", msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        listener.userEventTriggered(ctx, evt);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        IoSession session = SessionManager.self().removeServerSession(channel.id().toString());
        if (session == null) {
            return;
        }

        try {
            // 调用监听器
            listener.handlerRemoved(session);
        } catch (Exception e) {
            LogTopic.NET.error(e, "handlerRemoved", "channel", channel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        LogTopic.NET.warn("exceptionCaught", "channel", channel, "cause", cause);
        // TODO
        ctx.close();
    }

}
