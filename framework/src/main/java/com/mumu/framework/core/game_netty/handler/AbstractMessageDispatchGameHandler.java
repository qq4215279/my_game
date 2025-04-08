/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.handler;

import java.util.concurrent.TimeUnit;

import com.mumu.framework.core.cmd.CmdDispatch;
import com.mumu.framework.core.game_netty.channel.GameServerConfig;
import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelInboundHandler;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.server.MessageContext;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * AbstractGameMessageDispatchHandler
 * 抽象游戏消息分发处理器
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:35
 */
public abstract class AbstractMessageDispatchGameHandler<T> implements GameChannelInboundHandler {
    protected static final LogTopic log =LogTopic.ACTION ;

    protected long playerId;
    protected int gatewayServerId;

    protected CmdDispatch cmdDispatch;

    protected GameServerConfig serverConfig;

    /**  */
    private ScheduledFuture<?> flushToRedisScheduleFuture;
    /**  */
    private ScheduledFuture<?> flushToDBScheduleFuture;


    public AbstractMessageDispatchGameHandler() {
        this.cmdDispatch = SpringContextUtils.getBean(CmdDispatch.class);
        this.serverConfig = SpringContextUtils.getBean(GameServerConfig.class);
    }

    @Override
    public void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {
        this.playerId = playerId;
        GameChannelPromise initPromise = ctx.newPromise();
        initPromise.addListener(future -> {
            // 初始化成功之后，启动定时器，定时持久化数据
            fixTimerFlushPlayer(ctx);
            promise.setSuccess();
        });

        initData(ctx, playerId, initPromise);
    }

    @Override
    public void channelRead(AbstractGameChannelHandlerContext gameChannelHandlerContext, Object msg) throws Exception {
    }

    @Override
    public void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception {
        // 取消定时器
        if (flushToDBScheduleFuture != null) {
            flushToDBScheduleFuture.cancel(true);
        }
        if (flushToRedisScheduleFuture != null) {
            flushToRedisScheduleFuture.cancel(true);
        }
        this.updateToRedis0(ctx);
        this.updateToDB0(ctx);
        log.info("game channel 移除", "playerId", ctx.gameChannel().getPlayerId());

        // 向下一个Handler发送channel失效事件
        ctx.fireChannelInactive();
    }

    private void fixTimerFlushPlayer(AbstractGameChannelHandlerContext ctx) {
        // 获取定时器执行的延迟时间，单位是秒
        int flushRedisDelay = serverConfig.getFlushRedisDelaySecond();
        int flushDBDelay = serverConfig.getFlushDBDelaySeond();
        // 创建持久化数据到redis的定时任务
        flushToRedisScheduleFuture = ctx.executor().scheduleWithFixedDelay(() -> this.updateToRedis0(ctx), flushRedisDelay, flushRedisDelay, TimeUnit.SECONDS);
        flushToDBScheduleFuture = ctx.executor().scheduleWithFixedDelay(() -> this.updateToDB0(ctx), flushDBDelay, flushDBDelay, TimeUnit.SECONDS);
    }

    private void updateToRedis0(AbstractGameChannelHandlerContext ctx) {
        // TODO 任务开始执行的时间
        long start = System.currentTimeMillis();
        Promise<Boolean> promise = new DefaultPromise<>(ctx.executor());
        /* this.updateToRedis(promise).addListener(new GenericFutureListener<Future<Boolean>>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (future.isSuccess()) {
                     *//* if (log.isDebugEnabled()) {
                        long end = System.currentTimeMillis();
                        log.debug("player {} 同步数据到redis成功,耗时:{} ms", getPlayerId(), (end - start));
                    } *//*
                } else {
                    log.error("player {} 同步数据到Redis失败", getPlayerId());
                    // 这个时候应该报警
                }
            }
        }); */
    }

    private void updateToDB0(AbstractGameChannelHandlerContext ctx) {
        // TODO 任务开始执行时间
        long start = System.currentTimeMillis();
        Promise<Boolean> promise = new DefaultPromise<>(ctx.executor());
        /* updateToDB(promise).addListener(new GenericFutureListener<Future<Boolean>>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (future.isSuccess()) {
                     *//* if (log.isDebugEnabled()) {
                        long end = System.currentTimeMillis();
                        log.debug("player {} 同步数据到MongoDB成功,耗时:{} ms", getPlayerId(), (end - start));
                    } *//*
                } else {
                    log.error("player {} 同步数据到MongoDB失败", getPlayerId());
                    // 这个时候应该报警,将数据同步到日志中，以待恢复
                }
            }
        }); */
    }

    @Override
    public void userEventTriggered(AbstractGameChannelHandlerContext ctx, Object evt, Promise<Object> promise) throws Exception {
        // TODO
    }

    @Override
    public void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelReadRpcRequest(AbstractGameChannelHandlerContext ctx, MessageContext msg) throws Exception {
        // TODO
    }

    /**
     *
     * @param ctx ctx
     * @param playerId playerId
     * @param promise promise
     * @return void
     * @date 2024/6/26 16:40
     */
    protected abstract void initData(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise);


}
