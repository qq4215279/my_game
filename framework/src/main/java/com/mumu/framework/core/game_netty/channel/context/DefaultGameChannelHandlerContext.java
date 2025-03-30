/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.context;

import com.mumu.framework.core.game_netty.channel.handler.GameChannelHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelInboundHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelOutboundHandler;

import io.netty.util.concurrent.EventExecutor;

/**
 * DefaultGameChannelHandlerContext GameChannelHandler 默认上下文类
 * 
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:52
 */
public class DefaultGameChannelHandlerContext extends AbstractGameChannelHandlerContext {
    /**  */
    private final GameChannelHandler handler;

    public DefaultGameChannelHandlerContext(GameChannelPipeline pipeline, EventExecutor executor, String name,
        GameChannelHandler channelHandler) {
        // 判断一下这个channelHandler是处理接收消息的Handler还是处理发出消息的Handler
        super(pipeline, executor, name, isInbound(channelHandler), isOutbound(channelHandler));

        this.handler = channelHandler;
    }

    private static boolean isInbound(GameChannelHandler handler) {
        return handler instanceof GameChannelInboundHandler;
    }

    private static boolean isOutbound(GameChannelHandler handler) {
        return handler instanceof GameChannelOutboundHandler;
    }

    @Override
    public GameChannelHandler handler() {
        return this.handler;
    }

}
