/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.initializer;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.message.MessageHandlerListener;
import com.mumu.framework.core.mvc.servlet.handler.ConfirmHandler;
import com.mumu.framework.core.mvc.servlet.handler.DispatchServletHandler;
import com.mumu.framework.core.mvc.servlet.handler.HeartbeatHandler;
import com.mumu.framework.core.mvc.servlet.handler.RequestRateLimiterHandler;
import com.mumu.framework.core.mvc.servlet.handler.codec.JProtobufDecoder;
import com.mumu.framework.core.mvc.servlet.handler.codec.JProtobufEncoder;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * GatewayServletChannelInitializer
 * gateway ChannelInitializer
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:13
 */
public class GatewayServletChannelInitializer extends ChannelInitializer<Channel> {
    /** TODO 统一的消息处理器 */
    private final MessageHandlerListener messageHandlerListener;
    /** */
    private GatewayServerConfig serverConfig;
    /**  */
    private RateLimiter globalRateLimiter;

    public GatewayServletChannelInitializer(MessageHandlerListener messageHandlerListener, GatewayServerConfig serverConfig,
                                            RateLimiter globalRateLimiter) {
        this.messageHandlerListener = messageHandlerListener;
        this.serverConfig = serverConfig;
        this.globalRateLimiter = globalRateLimiter;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        // TODO 添加编码Handler
        p.addLast("EncodeHandler", new JProtobufEncoder());
        // 添加拆包
        p.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, -4, 0));
        // 添加解码
        p.addLast("DecodeHandler", new JProtobufDecoder());
        // p.addLast("DecodeHandler", new DecodeHandler());

        p.addLast("ConfirmHandler", new ConfirmHandler(serverConfig));
        // 添加限流handler
        p.addLast("RequestLimit", new RequestRateLimiterHandler(globalRateLimiter, serverConfig.getRequestPerSecond()));

        //
        int readerIdleTimeSeconds = serverConfig.getReaderIdleTimeSeconds();
        int writerIdleTimeSeconds = serverConfig.getWriterIdleTimeSeconds();
        int allIdleTimeSeconds = serverConfig.getAllIdleTimeSeconds();
        p.addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds));

        p.addLast("HeartbeatHandler", new HeartbeatHandler());
        p.addLast(new DispatchServletHandler(messageHandlerListener));
    }
}
