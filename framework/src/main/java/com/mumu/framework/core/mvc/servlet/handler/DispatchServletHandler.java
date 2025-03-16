package com.mumu.framework.core.mvc.servlet.handler;

import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.message.GameMessagePackage;
import com.mumu.framework.core.mvc.servlet.Request;
import com.mumu.framework.core.mvc.servlet.Response;
import com.mumu.framework.core.mvc.servlet.Servlet;
import com.mumu.framework.util.NettyUtil;
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

    public DispatchServletHandler(Servlet servlet) {
        this.servlet = servlet;
    }



    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!useSession) {
            super.channelActive(ctx);
            return;
        }

        // 为当前连接创建一个Session
        String sessionId = ctx.channel().attr(Netty4Constants.SESSIONID).get();
        if (null == sessionId) {
            Session session = SessionManager.getInstance().getSession(null, true);
            session.setPush(new TcpPush(ctx.channel()));
            ctx.channel().attr(Netty4Constants.SESSIONID).set(session.getId());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        if (msg instanceof RequestMessage) {
//            // 接受消息
//            RequestMessage message = (RequestMessage) msg;
//            message.setSessionId(ctx.channel().attr(Netty4Constants.SESSIONID).get());
//
//            Response response = new TcpResponse(ctx.channel());
//            Request request = new TcpRequest(ctx, context, ctx.channel(), message);
//            handle(request, response);

        if (msg instanceof GameMessagePackage gameMessagePackage) {
            int serviceId = gameMessagePackage.getHeader().getServiceId();
            // 如果首次通信，获取验证信息
            if (tokenBody == null) {
                ConfirmHandler confirmHandler = (ConfirmHandler)ctx.channel().pipeline().get("ConfirmHandler");
                tokenBody = confirmHandler.getTokenBody();
            }

            String clientIp = NettyUtil.getRemoteIP(ctx.channel());
            dispatchMessage(kafkaTemplate, ctx.executor(), playerServiceInstance, tokenBody.getPlayerId(), serviceId,
                    clientIp, gameMessagePackage, gatewayServerConfig);

            Request request = new Request(ctx.channel(), ctx, gameMessagePackage);
            Response response = new Response(ctx.channel());
            handle(request, response);
        }


    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
//        log.info("channel error, channel[bound:" + ctx.channel().isRegistered() +
//                        ", connected:" + ctx.channel().isActive() +
//                        ", open:" + ctx.channel().isOpen() +
//                        ", writable:" + ctx.channel().isWritable() + "]",
//                cause);

        // TODO
//        ctx.close();
//        log.error("服务器异常，连接{}断开", ctx.channel().id().asShortText(), cause);
    }


    /**
     * 处理Request
     * @param request
     * @param response
     */
    protected void handle(Request request, Response response) {
        servlet.service(request, response);
    }
}
