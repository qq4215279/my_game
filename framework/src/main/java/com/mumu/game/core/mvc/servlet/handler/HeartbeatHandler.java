/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.mvc.servlet.handler;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.JProtoBufUtil;
import com.mumu.game.proto.message.gate.core.HeartbeatMsgEC;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

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
    private final int maxHeartbeatCount = 66;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 在这里接收channel中的事件信息
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 一定时间内，既没有收到客户端信息，则断开连接
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                ctx.close();
               LogTopic.ACTION.info("连接读取空闲，断开连接", "channelId", ctx.channel().id().asShortText());
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // TODO 拦截心跳请求，并处理
       GameMessagePackage gameMessagePackage = (GameMessagePackage) msg;
       // 心跳协议
       if (gameMessagePackage.getHeader().getMessageId() == Cmd.HeartbeatMsg.getMessageId()) {
           LogTopic.ACTION.info("收到心跳信息", "channelId", ctx.channel().id().asShortText());

           HeartbeatMsgEC resMsg = new HeartbeatMsgEC();
           // 返回服务器时间
           resMsg.setSystemTime(System.currentTimeMillis());
           // TODO
           byte[] body = JProtoBufUtil.encode(resMsg);

           GameMessagePackage packageMsg = new GameMessagePackage();
           // TODO 封装header
           GameMessageHeader header = Cmd.HeartbeatMsg.createGameMessageHeader(false);
           header.setSeq(gameMessagePackage.getHeader().getSeq());

           packageMsg.setHeader(header);
           packageMsg.setBody(body);

           // 返回消息
           ctx.writeAndFlush(packageMsg);

           this.heartbeatCount++;
           // 超时断开连接
           if (this.heartbeatCount > maxHeartbeatCount) {
               ctx.close();
           }

           // 收到非心跳消息之后，重新计数
       } else {
           this.heartbeatCount = 0;
           ctx.fireChannelRead(msg);
       }
    }
}
