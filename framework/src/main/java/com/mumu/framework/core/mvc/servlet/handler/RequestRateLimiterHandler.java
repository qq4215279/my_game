/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler;

import com.mumu.framework.core.log.LogTopic;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.common.proto.message.system.message.GameMessagePackage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * RequestRateLimiterHandler
 * 游戏服务器网关流量限制Handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:41
 */
public class RequestRateLimiterHandler extends ChannelInboundHandlerAdapter {
    private static final LogTopic log = LogTopic.ACTION;


    /** 全局限制器 */
    private RateLimiter globalRateLimiter;
    /** 用户限流器，用于限制单个用户的请求。 */
    private static RateLimiter userRateLimiter;

    private int lastClientSeqId = 0;

    public RequestRateLimiterHandler(RateLimiter globalRateLimiter, double requestPerSecond) {
        this.globalRateLimiter = globalRateLimiter;
        this.userRateLimiter = RateLimiter.create(requestPerSecond);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
       // 获取令牌失败，触发限流
       if (!userRateLimiter.tryAcquire()) {
           log.info("请求过多，连接断开", "channel", ctx.channel().id().asShortText());
           ctx.close();
           return;
       }

       // 获取全局令牌失败，触发限流
       if (!globalRateLimiter.tryAcquire()) {
           log.info("全局请求超载，断开", "channel", ctx.channel().id().asShortText());
           ctx.close();
           return;
       }

       // 6.7.3 消息幂等处理 p167
       GameMessagePackage gameMessagePackage = (GameMessagePackage) msg;
       int clientSeqId = gameMessagePackage.getHeader().getClientSeqId();
       if (lastClientSeqId > 0) {
           // 直接返回，不再处理。
           if (clientSeqId <= lastClientSeqId) {
               return;
           }
       }

       this.lastClientSeqId = clientSeqId;
       // 不要忘记添加这个，要不然后面的handler收不到消息
       ctx.fireChannelRead(msg);
    }
}
