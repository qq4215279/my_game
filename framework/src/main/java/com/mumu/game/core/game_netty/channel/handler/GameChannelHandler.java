/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.game_netty.channel.handler;

import com.mumu.game.core.game_netty.channel.context.AbstractGameChannelHandlerContext;

/**
 * GameChannelHandler
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:55
 */
public interface GameChannelHandler {
    /**
     *
     * @param ctx ctx
     * @param cause cause
     */
    void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception;
}
