package com.mumu.framework.mvc.servlet.handler;

import com.mumu.framework.mvc.message.GameMessagePackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * HeartbeatHandler
 * 连接心跳检测Handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:41
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    /** 心跳计数器，如果一直接收到的是心跳消息，达到一定数量之后，说明客户端一直没有用户操作了，服务器就主动断开连接。 */
    private int heartbeatCount = 0;
    /** 最大心跳数 */
    private int maxHeartbeatCount = 66;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 在这里接收channel中的事件信息
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 一定时间内，既没有收到客户端信息，则断开连接
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                ctx.close();
//                logger.debug("连接读取空闲，断开连接，channelId:{}", ctx.channel().id().asShortText());
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 拦截心跳请求，并处理
//        GameMessagePackage gameMessagePackage = (GameMessagePackage) msg;
//        if (gameMessagePackage.getHeader().getMessageId() == GatewayMessageCode.Heartbeat.getMessageId()) {
//            logger.debug("收到心跳信息,channelid:{}", ctx.channel().id().asShortText());
//            HeartbeatMsgResponse response = new HeartbeatMsgResponse();
//            // 返回服务器时间
//            response.getBodyObj().setServerTime(System.currentTimeMillis());
//            GameMessagePackage returnPackage = new GameMessagePackage();
//            response.getHeader().setClientSeqId(gameMessagePackage.getHeader().getClientSeqId());
//            returnPackage.setHeader(response.getHeader());
//            returnPackage.setBody(response.body());
//            ctx.writeAndFlush(returnPackage);
//            this.heartbeatCount++;
//            if (this.heartbeatCount > maxHeartbeatCount) {
//                ctx.close();
//            }
//
//        } else {
//            // 收到非心跳消息之后，重新计数
//            this.heartbeatCount = 0;
//            ctx.fireChannelRead(msg);
//        }
    }
}
