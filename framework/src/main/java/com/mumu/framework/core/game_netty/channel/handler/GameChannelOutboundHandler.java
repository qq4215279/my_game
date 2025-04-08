/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.handler;

import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.mvc.server.MessageContext;

import io.netty.util.concurrent.Promise;

/**
 * GameChannelOutboundHandler
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:57
 */
public interface GameChannelOutboundHandler extends GameChannelHandler {

    /**
     *
     * @param ctx ctx
     * @param msg msg
     * @param promise promise
     * @return void
     * @date 2024/6/19 19:12
     */
    void writeAndFlush(AbstractGameChannelHandlerContext ctx, ResponseResult msg, GameChannelPromise promise) throws Exception;

    /**
     *
     * @param ctx ctx
     * @param gameMessage gameMessage
     * @param callback callback
     * @return void
     * @date 2024/6/19 19:13
     */
    void writeRPCMessage(AbstractGameChannelHandlerContext ctx, MessageContext gameMessage, Promise<MessageContext> callback);

    /**
     *
     * @param ctx ctx
     * @param promise promise
     * @return void
     * @date 2024/6/19 19:21
     */
    void close(AbstractGameChannelHandlerContext ctx, GameChannelPromise promise);

}
