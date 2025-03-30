/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.utils.JWTUtil;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.servlet.DispatchServlet;
import com.mumu.framework.core.mvc.servlet.Request;
import com.mumu.framework.core.mvc.servlet.Response;
import com.mumu.framework.core.mvc.servlet.Servlet;

import com.mumu.framework.core.mvc.servlet.session.PlayerSessionManager;
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

    /**  */
    private final Servlet servlet;

    /** 注入游戏网关服务配置信息。 */
    private GatewayServerConfig gatewayServerConfig;

    public DispatchServletHandler() {
        this.servlet = new DispatchServlet();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
       // if (msg instanceof RequestMessage) {
       //     // 接受消息
       //     RequestMessage message = (RequestMessage) msg;
       //     message.setSessionId(ctx.channel().attr(Netty4Constants.SESSIONID).get());
       //
       //     Response response = new TcpResponse(ctx.channel());
       //     Request request = new TcpRequest(ctx, context, ctx.channel(), message);
       //     handle(request, response);

       GameMessagePackage gameMessagePackage = (GameMessagePackage)msg;
       int serviceId = gameMessagePackage.getHeader().getServiceId();


       String clientIp = IoSession.getRemoteIP(ctx.channel());
       dispatchMessage(kafkaTemplate, ctx.executor(), playerServiceInstance, tokenBody.getPlayerId(), serviceId,
           clientIp, gameMessagePackage, gatewayServerConfig);

       Request request = new Request(ctx.channel(), ctx, gameMessagePackage);
       Response response = new Response(ctx.channel());
       handle(request, response);

       servlet.doCommand(ctx, msg);

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        LogTopic.NET.warn("exceptionCaught", "channel", channel, "cause", cause);
        // TODO
       ctx.close();
    }

}
