package com.mumu.framework.mvc.servlet.handler;

import com.mumu.framework.mvc.GatewayServerConfig;
import com.mumu.framework.mvc.message.GameMessagePackage;
import com.mumu.framework.mvc.servlet.Servlet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * DispatchServletHandler
 * 消息分发处理器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:39
 */
public class DispatchServletHandler extends ChannelInboundHandlerAdapter {

    private Servlet servlet;

    /** 注入游戏网关服务配置信息。 */
    private GatewayServerConfig gatewayServerConfig;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        GameMessagePackage gameMessagePackage = (GameMessagePackage)msg;
//        int serviceId = gameMessagePackage.getHeader().getServiceId();
//        // 如果首次通信，获取验证信息
//        if (tokenBody == null) {
//            ConfirmHandler confirmHandler = (ConfirmHandler)ctx.channel().pipeline().get("ConfirmHandler");
//            tokenBody = confirmHandler.getTokenBody();
//        }
//
//        String clientIp = NettyUtils.getRemoteIP(ctx.channel());
//        dispatchMessage(kafkaTemplate, ctx.executor(), playerServiceInstance, tokenBody.getPlayerId(), serviceId,
//                clientIp, gameMessagePackage, gatewayServerConfig);
    }
}
