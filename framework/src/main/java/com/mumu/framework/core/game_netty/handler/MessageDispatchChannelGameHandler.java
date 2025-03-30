/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.handler;

import java.util.concurrent.TimeUnit;

import com.mumu.framework.business.player.domain.Player;
import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * MessageDispatchChannelGameHandler
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:56
 */
public class MessageDispatchChannelGameHandler extends AbstractMessageDispatchGameHandler {
    private Player player;

    private ScheduledFuture<?> flushToRedisScheduleFuture;
    private ScheduledFuture<?> flushToDBScheduleFuture;

    public MessageDispatchChannelGameHandler() {
        super();
    }

    // 9.1.2 异步加载游戏数据实现 p249
    @Override
    public void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {
        // 在用户GameChannel注册的时候，对用户的数据进行初始化
        /* playerDao.findPlayer(playerId, new DefaultPromise<>(ctx.executor()))
                .addListener(new GenericFutureListener<Future<Optional<Player>>>() {
                    @Override
                    public void operationComplete(Future<Optional<Player>> future) throws Exception {
                        Optional<Player> playerOp = future.get();
                        if (playerOp.isPresent()) {
                            player = playerOp.get();
                            playerManager = new PlayerManager(player);
                            promise.setSuccess();

                            // 启动定时持久化数据到数据库
                            fixTimerFlushPlayer(ctx);
                        } else {
                            logger.error("player {} 不存在", playerId);
                            promise.setFailure(new IllegalArgumentException("找不到Player数据，playerId:" + playerId));
                        }
                    }
                }); */

    }

    /**
     * 9.2.3 数据定时异步持久化实现 p254
     *
     * @param ctx ctx
     * @return void
     * @date 2024/6/24 19:16
     */
    private void fixTimerFlushPlayer(AbstractGameChannelHandlerContext ctx) {
        // 获取定时器执行的延迟时间，单位是秒
        int flushRedisDelay = serverConfig.getFlushRedisDelaySecond();
        int flushDBDelay = serverConfig.getFlushDBDelaySeond();

        // 创建持久化数据到redis的定时任务
        flushToRedisScheduleFuture = ctx.executor().scheduleWithFixedDelay(() -> {
            // 任务开始执行的时间
            long start = System.currentTimeMillis();
            Promise<Boolean> promise = new DefaultPromise<>(ctx.executor());
            /* playerDao.saveOrUpdatePlayerToRedis(player, promise)
                    .addListener(new GenericFutureListener<Future<Boolean>>() {
                        @Override
                        public void operationComplete(Future<Boolean> future) throws Exception {
                            if (future.isSuccess()) {
                                if (logger.isDebugEnabled()) {
                                    long end = System.currentTimeMillis();
                                    logger.debug("player {} 同步数据到redis成功,耗时:{} ms", player.getPlayerId(), (end - start));
                                }
                            } else {
                                logger.error("player {} 同步数据到Redis失败", player.getPlayerId());
                                // 这个时候应该报警
                            }
                        }
                    }); */
        }, flushRedisDelay, flushRedisDelay, TimeUnit.SECONDS);


        flushToDBScheduleFuture = ctx.executor().scheduleWithFixedDelay(() -> {
            long start = System.currentTimeMillis();// 任务开始执行时间
            Promise<Boolean> promise = new DefaultPromise<>(ctx.executor());
            /* playerDao.saveOrUpdatePlayerToDB(player, promise).addListener(new GenericFutureListener<Future<Boolean>>() {
                @Override
                public void operationComplete(Future<Boolean> future) throws Exception {
                    if (future.isSuccess()) {
                        if (logger.isDebugEnabled()) {
                            long end = System.currentTimeMillis();
                            logger.debug("player {} 同步数据到MongoDB成功,耗时:{} ms", player.getPlayerId(), (end - start));
                        }
                    } else {
                        logger.error("player {} 同步数据到MongoDB失败", player.getPlayerId());
                        // 这个时候应该报警,将数据同步到日志中，以待恢复
                    }
                }
            }); */
        }, flushDBDelay, flushDBDelay, TimeUnit.SECONDS);

    }

    /**
     * 9.2.3 数据定时异步持久化实现 p255
     * @param ctx ctx
     * @return void
     * @date 2024/6/24 19:16
     */
    @Override
    public void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception {
        // 取消定时器
        if (flushToDBScheduleFuture != null) {
            flushToDBScheduleFuture.cancel(true);
        }
        if (flushToRedisScheduleFuture != null) {
            flushToRedisScheduleFuture.cancel(true);
        }

        // GameChannel移除的时候，强制更新一次数据
        // this.playerDao.syncFlushPlayer(player);
        log.info("强制flush player {} 成功", player.getPlayerId());
        log.info("game channel 移除", "playerId", ctx.gameChannel().getPlayerId());

        // 向下一个Handler发送channel失效事件
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(AbstractGameChannelHandlerContext gameChannelHandlerContext, Object msg) throws Exception {
        super.channelRead(gameChannelHandlerContext, msg);
    }

    @Override
    public void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务器异常", "playerId", ctx.gameChannel().getPlayerId(), cause);
    }

    @Override
    protected void initData(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {

    }


}
