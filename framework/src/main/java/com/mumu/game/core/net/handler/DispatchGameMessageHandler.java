package com.mumu.game.core.net.handler;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.mvc.server.IoSession;
import com.mumu.game.core.mvc.server.MessageContext;
import com.mumu.game.core.mvc.session.SessionManager;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * DispatchGameMessageHandler
 * 分发游戏消息处理器
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:10
 */
@ChannelHandler.Sharable
public class DispatchGameMessageHandler extends ChannelInboundHandlerAdapter {
    /** 消息处理监听器 */
    private final MessageHandlerListener listener;

    private final ServerInfo serverInfo;

    public DispatchGameMessageHandler(MessageHandlerListener listener, ServerInfo serverInfo) {
        this.listener = listener;
        this.serverInfo = serverInfo;
    }

    /** 客户端首次连接服务器时，发送握手消息（握手请求不在此发送） */
    private void clientFirstConnect(IoSession session) {
        if (session.hasAttr(NetConstants.SESSION_CLIENT)) {
            LogTopic.NET.info(
                    "clientFirstConnect",
                    "connectServiceType",
                    session.getAttr(NetConstants.SESSION_SERVICE_TYPE),
                    "currServerInfo",
                    serverInfo);
            // TODO 发送本服信息进行握手
            // MessageSender.send(session, Cmd.ReqServerInfoHandshake, serverInfo.build());
        }
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
            // 客户端首次连接服务器时，则推送本服信息
            // clientFirstConnect(session);
            // 调用监听器
            listener.handleActive(session);
        } catch (Exception e) {
            LogTopic.NET.error(e, "channelActive", "channel", channel);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GameMessagePackage gameMessagePackage = (GameMessagePackage)msg;
        Channel channel = ctx.channel();
        IoSession session = SessionManager.self().get(channel.id().toString());
        if (session == null) {
            LogTopic.NET.warn("channelRead", "session is null", "channel", channel);
            return;
        }
        try {
            listener.handleRead(MessageContext.of(gameMessagePackage, session));
        } catch (Exception e) {
            LogTopic.NET.error(e, "channelRead", "channel", channel, "msg", msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        LogTopic.NET.warn("exceptionCaught", "channel", channel, "cause", cause);
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        listener.userEventTriggered(ctx, evt);
    }
}

