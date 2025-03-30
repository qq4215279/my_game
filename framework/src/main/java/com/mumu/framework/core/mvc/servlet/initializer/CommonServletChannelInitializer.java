/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.initializer;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.servlet.handler.HeartbeatHandler;
import com.mumu.framework.core.mvc.servlet.handler.codec.DecodeHandler;
import com.mumu.framework.core.mvc.servlet.handler.codec.EncodeHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * CommonServletChannelInitializer
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/29 11:31
 */
public class CommonServletChannelInitializer extends ChannelInitializer<Channel> {
    /** */
    private GatewayServerConfig serverConfig;
    /**  */
    private RateLimiter globalRateLimiter;

    public CommonServletChannelInitializer(GatewayServerConfig serverConfig, RateLimiter globalRateLimiter) {
        this.serverConfig = serverConfig;
        this.globalRateLimiter = globalRateLimiter;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        // 添加编码Handler
        p.addLast("EncodeHandler", new EncodeHandler(serverConfig));
        // 添加拆包
        p.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, -4, 0));
        // 添加解码
        p.addLast("DecodeHandler", new DecodeHandler());

        //
        int readerIdleTimeSeconds = serverConfig.getReaderIdleTimeSeconds();
        int writerIdleTimeSeconds = serverConfig.getWriterIdleTimeSeconds();
        int allIdleTimeSeconds = serverConfig.getAllIdleTimeSeconds();
        p.addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds));

        p.addLast("HeartbeatHandler", new HeartbeatHandler());
        // p.addLast(new DispatchGameMessageHandler(kafkaTemplate, playerServiceInstance, serverConfig));
    }
}
