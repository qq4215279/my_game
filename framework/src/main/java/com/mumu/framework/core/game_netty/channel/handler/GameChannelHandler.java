/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.handler;

import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;

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
