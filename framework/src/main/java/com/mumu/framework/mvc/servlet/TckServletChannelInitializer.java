package com.mumu.framework.mvc.servlet;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.framework.mvc.GatewayServerConfig;
import com.mumu.framework.mvc.servlet.handler.ConfirmHandler;
import com.mumu.framework.mvc.servlet.handler.HeartbeatHandler;
import com.mumu.framework.mvc.servlet.handler.RequestRateLimiterHandler;
import com.mumu.framework.mvc.servlet.handler.codec.DecodeHandler;
import com.mumu.framework.mvc.servlet.handler.codec.EncodeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * TckServletChannelInitializer
 *
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:13
 */
public class TckServletChannelInitializer extends ChannelInitializer<Channel> {

    private GatewayServerConfig serverConfig;
    private RateLimiter globalRateLimiter;

    public TckServletChannelInitializer(GatewayServerConfig serverConfig, RateLimiter globalRateLimiter) {
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
//        p.addLast("ConfirmHandler", new ConfirmHandler(serverConfig, channelService, kafkaTemplate, applicationContext));
//        // 添加限流handler
//        p.addLast("RequestLimit", new RequestRateLimiterHandler(globalRateLimiter, serverConfig.getRequestPerSecond()));
//
//        int readerIdleTimeSeconds = serverConfig.getReaderIdleTimeSeconds();
//        int writerIdleTimeSeconds = serverConfig.getWriterIdleTimeSeconds();
//        int allIdleTimeSeconds = serverConfig.getAllIdleTimeSeconds();
//        p.addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds));
//
//        p.addLast("HeartbeatHandler", new HeartbeatHandler());
//        p.addLast(new DispatchGameMessageHandler(kafkaTemplate, playerServiceInstance, serverConfig));
    }
}
