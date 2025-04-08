/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.handler;

import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.mvc.server.MessageContext;

import io.netty.util.concurrent.Promise;

/**
 * GameChannelInboundHandler
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:56
 */
public interface GameChannelInboundHandler  extends GameChannelHandler {

    /**
     * GameChannel第一次注册的时候调用
     * @param ctx ctx
     * @param playerId playerId
     * @param promise promise
     * @return void
     * @date 2024/6/19 19:12
     */
    void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise);

    /**
     * GameChannel被移除的时候调用
     * @param ctx ctx
     * @return void
     * @date 2024/6/19 19:12
     */
    void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception;

    /**
     * 读取并处理客户端发送的消息
     * @param ctx ctx
     * @param msg msg
     * @return void
     * @date 2024/6/19 19:12
     */
    void channelRead(AbstractGameChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * 触发一些内部事件
     * @param ctx ctx
     * @param evt evt
     * @param promise promise
     * @return void
     * @date 2024/6/19 19:12
     */
    void userEventTriggered(AbstractGameChannelHandlerContext ctx, Object evt, Promise<Object> promise) throws Exception;

    /**
     * 读取并处理RPC的请求消息
     * @param ctx ctx
     * @param msg msg
     * @return void
     * @date 2024/6/19 19:42
     */
    void channelReadRpcRequest(AbstractGameChannelHandlerContext ctx, MessageContext msg) throws Exception;

}
