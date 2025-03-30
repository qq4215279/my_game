/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel;

import java.util.HashMap;
import java.util.Map;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.thread.GameEventExecutorGroup;
import com.mumu.framework.core.cloud.GameChannelCloseEvent;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * GameMessageDispatchServlet
 * 游戏消息分发servlet处理器
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:59
 */
public class GameMessageDispatchServlet {
    private static final LogTopic log = LogTopic.ACTION;

    /** 管理PlayerId与GameChannel的集合 */
    private Map<Long, GameChannel> gameChannelGroupMap = new HashMap<>();

    /** 当前管理gameChannelGroup集合的事件线程池 */
    private EventExecutor executor;
    /** 业务处理线程池组 */
    private GameEventExecutorGroup workerGroup;
    /**  */
    private GameChannelInitializer channelInitializer;


    public GameMessageDispatchServlet(GameEventExecutorGroup workerGroup,
                                      GameChannelInitializer channelInitializer) {
        this.executor = workerGroup.next();
        this.workerGroup = workerGroup;
        this.channelInitializer = channelInitializer;
    }

    /**
     * 获取GameChannel 并注册
     * @param playerId playerId
     * @return com.mumu.framework.core.game_netty.channel.GameChannel
     * @author liuzhen
     * @date 2025/3/30 22:08
     */
    private GameChannel getGameChannel(long playerId) {
        GameChannel gameChannel = this.gameChannelGroupMap.get(playerId);
        // 从集合中获取一个GameChannel，如果这个GameChannel为空，则重新创建，并初始化注册这个Channel，完成GameChannel的初始化。
        if (gameChannel == null) {
            gameChannel = new GameChannel(playerId, this);
            this.gameChannelGroupMap.put(playerId, gameChannel);
            // 初始化Channel，可以通过这个接口向GameChannel中添加处理消息的Handler.
            this.channelInitializer.initChannel(gameChannel);
            // 发注册GameChannel的事件。
            gameChannel.register(workerGroup.select(playerId), playerId);
        }
        return gameChannel;
    }

    /**
     * 发送GameChannel失效的事件，在这个事件中可以处理一些数据落地的操作
     * @param playerId playerId
     * @return void
     * @date 2024/6/19 19:21
     */
    public void fireInactiveChannel(long playerId) {
        this.safeExecute(() -> {
            GameChannel gameChannel = this.gameChannelGroupMap.remove(playerId);
            if (gameChannel != null) {
                gameChannel.fireChannelInactive();

                // 发布GameChannel失效事件
                GameChannelCloseEvent event = new GameChannelCloseEvent(this, playerId);
                SpringContextUtils.publishEvent(event);
            }
        });
    }

    /**
     * 发送接收到的消息事件
     * @param playerId playerId
     * @param message message
     * @return void
     * @date 2024/6/19 19:21
     */
    public void fireReadGameMessage(long playerId, GameMessagePackage message) {
        this.safeExecute(() -> {
            GameChannel gameChannel = this.getGameChannel(playerId);
            gameChannel.fireReadGameMessage(message);
        });
    }

    /**
     * 发送用户定义的事件
     * @param playerId playerId
     * @param msg msg
     * @param promise promise
     * @return void
     * @date 2024/6/19 19:21
     */
    public void fireUserEvent(long playerId, Object msg, Promise<Object> promise) {
        this.safeExecute(() -> {
            GameChannel gameChannel = this.getGameChannel(playerId);
            gameChannel.fireUserEvent(msg, promise);
        });
    }

    /**
     * 发送消息广播事件，客多个客户端发送消息。
     * @param gameMessage gameMessage
     * @param playerIds playerIds
     * @return void
     * @date 2024/6/19 19:21
     */
    public void broadcastMessage(ResponseResult gameMessage, long... playerIds) {
        if (playerIds == null || playerIds.length == 0) {
            log.info("广播的对象集合为空，直接返回");
            return;
        }

        this.safeExecute(() -> {
            for (long playerId : playerIds) {
                if (this.gameChannelGroupMap.containsKey(playerId)) {
                    GameChannel gameChannel = this.getGameChannel(playerId);
                    gameChannel.pushMessage(gameMessage);
                }
            }
        });
    }

    /**
     * 广播所有玩家
     * @param gameMessage gameMessage
     * @return void
     * @date 2024/6/26 15:13
     */
    public void broadcastMessage(ResponseResult gameMessage) {
        this.safeExecute(() -> {
            this.gameChannelGroupMap.values().forEach(channel -> {
                channel.pushMessage(gameMessage);
            });
        });
    }

    /**
     * 此方法保证所有操作gameChannelGroup集合的行为都在同一个线程中执行，避免跨线程操作。
     * 将方法的请求变成事件，在此类所属的事件线程池中执行
     * @param task task
     * @return void
     * @date 2024/6/19 19:19
     */
    private void safeExecute(Runnable task) {
        // 如果当前调用这个方法的线程和此类所属的线程是同一个线程，则可以立刻执行执行。
        if (this.executor.inEventLoop()) {
            try {
                task.run();
            } catch (Throwable e) {
                log.error("服务器内部错误", e);
            }
        } else {
            // 如果当前调用这个方法的线程和此类所属的线程不是同一个线程，将此任务提交到线程池中等待执行。
            this.executor.execute(() -> {
                try {
                    task.run();
                } catch (Throwable e) {
                    log.error("服务器内部错误", e);
                }
            });
        }
    }

}
