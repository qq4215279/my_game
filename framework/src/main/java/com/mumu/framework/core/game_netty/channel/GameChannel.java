/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel;

import java.util.ArrayList;
import java.util.List;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.channel.context.GameChannelPipeline;
import com.mumu.framework.core.game_netty.channel.future.DefaultGameChannelPromise;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.server.MessageContext;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.Getter;

/**
 * GameChannel
 * 游戏channel
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:49
 */
@Getter
public class GameChannel {
    private static final LogTopic log = LogTopic.ACTION;

    /** 玩家id */
    private final long playerId;
    /** 网关serverId */
    private int gatewayServerId;
    /** 是否关闭 */
    private boolean isClose;

    /** 事件分发管理器 */
    private GameMessageDispatchServlet gameMessageDispatchServlet;

    /** 处理事件的链表 */
    private GameChannelPipeline channelPipeline;
    /** 服务器配置 */
    private GameServerConfig serverConfig;

    /** 此channel所属的线程 */
    private volatile EventExecutor executor;
    /** 标记GameChannel是否注册成功 */
    private volatile boolean registered;
    /** 事件等待队列，如果GameChannel还没有注册成功，这个时候又有新的消息过来了，就让事件在这个队列中等待。 */
    private List<Runnable> waitTaskList = new ArrayList<>(5);


    public GameChannel(long playerId, GameMessageDispatchServlet gameMessageDispatchServlet) {
        this.playerId = playerId;
        this.gameMessageDispatchServlet = gameMessageDispatchServlet;

        channelPipeline = new GameChannelPipeline(this);
        this.serverConfig = SpringContextUtils.getBean(GameServerConfig.class);
    }

    /**
     * 注册
     * @param executor executor
     * @param playerId playerId
     * @return void
     * @date 2024/6/26 14:40
     */
    public void register(EventExecutor executor, long playerId) {
        this.executor = executor;
        GameChannelPromise promise = new DefaultGameChannelPromise(this);
        this.channelPipeline.fireRegister(playerId, promise);
        promise.addListener(future -> {
            // 注册成功的时候，设置为true
            if (future.isSuccess()) {
                registered = true;
                waitTaskList.forEach(task -> {
                    // 注册channel成功之后，执行等待的任务，因为此执行这些任务和判断是否注册完成是在同一个线程中，所以此处执行完之后，waitTaskList中不会再有新的任务了。
                    task.run();
                });
            } else {
                gameMessageDispatchServlet.fireInactiveChannel(playerId);
                log.error("player {} channel 注册失败", playerId, future.cause());
            }
        });
    }

    /**
     *
     * @return void
     * @date 2024/6/26 14:42
     */
    public void fireChannelInactive() {
        this.safeExecute(() -> {
            this.channelPipeline.fireChannelInactive();
        });
    }

    /**
     *
     * @param context context
     * @return void
     * @date 2024/6/26 14:42
     */
    public void fireReadGameMessage(MessageContext context) {
        this.safeExecute(() -> {
            // channel已关闭，不再接收消息
            if (isClose) {
                return;
            }

            GameMessagePackage gameMessage = context.getProxy();
            this.gatewayServerId = gameMessage.getHeader().getFromServerId();
            this.channelPipeline.fireChannelRead(context);
        });
    }

    /**
     *
     * @param message message
     * @param promise promise
     * @return void
     * @date 2024/6/26 14:42
     */
    public void fireUserEvent(Object message, Promise<Object> promise) {
        this.safeExecute(() -> {
            this.channelPipeline.fireUserEventTriggered(message, promise);
        });
    }

    /**
     *
     * @param gameMessage gameMessage
     * @return void
     * @date 2024/6/26 14:42
     */
    public void fireChannelReadRPCRequest(MessageContext gameMessage) {
        this.safeExecute(() -> {
            this.channelPipeline.fireChannelReadRPCRequest(gameMessage);
        });
    }

    /**
     *
     * @param gameMessage gameMessage
     * @return void
     * @date 2024/6/26 14:42
     */
    public void pushMessage(ResponseResult gameMessage) {
        safeExecute(() -> {
            this.channelPipeline.writeAndFlush(gameMessage);
        });
    }

    /**
     * 执行任务
     * @param task task
     * @return void
     * @date 2024/6/26 15:27
     */
    private void safeExecute(Runnable task) {
        if (this.executor.inEventLoop()) {
            this.safeExecute0(task);
        } else {
            this.executor.execute(() -> {
                this.safeExecute0(task);
            });
        }
    }

    /**
     * do 执行任务
     * @param task task
     * @return void
     * @date 2024/6/26 15:27
     */
    private void safeExecute0(Runnable task) {
        try {
            if (!this.registered) {
                waitTaskList.add(task);
            } else {
                task.run();
            }
        } catch (Throwable e) {
            log.error("服务器异常", e);
        }
    }

    /**
     *
     * @param gameMessagePackage gameMessagePackage
     * @param promise promise
     * @return void
     * @date 2024/6/26 14:43
     */
    public void unsafeSendMessage(GameMessagePackage gameMessagePackage, GameChannelPromise promise) {
        // this.messageSendFactory.sendMessage(gameMessagePackage, promise);
    }

    /**
     *
     * @param gameMessage gameMessage
     * @param callback callback
     * @return void
     * @date 2024/6/26 14:43
     */
    public void unsafeSendRpcMessage(MessageContext gameMessage, Promise<MessageContext> callback) {
        // TODO
        /* if (gameMessage.getHeader().getMessageType() == EnumMesasageType.RPC_REQUEST) {
            this.gameRpcService.sendRPCRequest(gameMessage, callback);
        } else if (gameMessage.getHeader().getMessageType() == EnumMesasageType.RPC_RESPONSE) {
            this.gameRpcService.sendRPCResponse(gameMessage);
        } */
    }


    public void unsafeClose() {
        this.gameMessageDispatchServlet.fireInactiveChannel(playerId);
    }


    public EventExecutor executor() {
        return executor;
    }
}
