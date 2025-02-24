package com.mumu.framework.mvc.servlet.handler;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.framework.mvc.message.GameMessagePackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RequestRateLimiterHandler
 * 游戏服务器网关流量限制Handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:41
 */
public class RequestRateLimiterHandler extends ChannelInboundHandlerAdapter {
    /** 全局限制器 */
    private RateLimiter globalRateLimiter;
    /** 用户限流器，用于限制单个用户的请求。 */
    private static RateLimiter userRateLimiter;
    private static Logger logger = LoggerFactory.getLogger(RequestRateLimiterHandler.class);
    private int lastClientSeqId = 0;

    public RequestRateLimiterHandler(RateLimiter globalRateLimiter, double requestPerSecond) {
        this.globalRateLimiter = globalRateLimiter;
        userRateLimiter = RateLimiter.create(requestPerSecond);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        // 获取令牌失败，触发限流
//        if (!userRateLimiter.tryAcquire()) {
//            logger.debug("channel {} 请求过多，连接断开", ctx.channel().id().asShortText());
//            ctx.close();
//            return;
//        }
//
//        // 获取全局令牌失败，触发限流
//        if (!globalRateLimiter.tryAcquire()) {
//            logger.debug("全局请求超载，channel {} 断开", ctx.channel().id().asShortText());
//            ctx.close();
//            return;
//        }
//
//        // 6.7.3 消息幂等处理 p167
//        GameMessagePackage gameMessagePackage = (GameMessagePackage) msg;
//        int clientSeqId = gameMessagePackage.getHeader().getClientSeqId();
//        if (lastClientSeqId > 0) {
//            // 直接返回，不再处理。
//            if (clientSeqId <= lastClientSeqId) {
//                return;
//            }
//        }
//
//        this.lastClientSeqId = clientSeqId;
//        // 不要忘记添加这个，要不然后面的handler收不到消息
//        ctx.fireChannelRead(msg);
    }
}
