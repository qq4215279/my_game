/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler;

import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.proto.message.system.message.MessageTypeEnum;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.cloud.PlayerServiceManager;
import com.mumu.framework.core.cloud.ServiceType;
import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.game_netty.context.GameMessageConsumerManager;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.servlet.session.PlayerSessionManager;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * GateDispatchServletHandler
 * gateway消息分发处理器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:39
 */
public class GateDispatchServletHandler extends ChannelInboundHandlerAdapter {
    private static final LogTopic log = LogTopic.NET;

    private final PlayerServiceManager playerServiceManager;

    /**  */
    private final GameMessageConsumerManager gameMessageConsumerManager;

    /** 注入游戏网关服务配置信息。 */
    private GatewayServerConfig gatewayServerConfig;

    public GateDispatchServletHandler() {
        this.playerServiceManager = SpringContextUtils.getBean(PlayerServiceManager.class);
        this.gameMessageConsumerManager = SpringContextUtils.getBean(GameMessageConsumerManager.class);
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
        GameMessageHeader header = gameMessagePackage.getHeader();
        int serviceId = header.getServiceId();
       long playerId = gameMessagePackage.getHeader().getPlayerId();

       IoSession session = PlayerSessionManager.self().getChannel(playerId);
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


       // String clientIp = IoSession.getRemoteIP(ctx.channel());
       // dispatchMessage(kafkaTemplate, ctx.executor(), playerServiceInstance, tokenBody.getPlayerId(), serviceId,
       //     clientIp, gameMessagePackage, gatewayServerConfig);
       //
       // Request request = new Request(ctx.channel(), ctx, gameMessagePackage);
       // Response response = new Response(ctx.channel());
       // handle(request, response);
       //
       // servlet.doCommand(ctx, msg);

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
