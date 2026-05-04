/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.game_netty.context;

import org.springframework.stereotype.Component;

import com.mumu.game.core.cmd.CmdDispatch;
import com.mumu.game.core.game_netty.channel.GameChannelInitializer;
import com.mumu.game.core.game_netty.channel.GameMessageDispatchServlet;
import com.mumu.game.core.game_netty.channel.GameServerConfig;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.thread.GameEventExecutorGroup;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import jakarta.annotation.Resource;

/**
 * GameMessageConsumerManager 游戏消息管理器
 * 
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:39
 */
@Component
public class GameMessageConsumerManager {
    private static final LogTopic log = LogTopic.ACTION;

    /** GameChannel的一些配置信息 */
    @Resource
    private GameServerConfig serverConfig;
    /** 消息管理类，负责管理根据消息id，获取对应的消息类实例 */
    @Resource
    private CmdDispatch cmdDispatch;
    // @Resource
    // private PlayerServiceManager playerServiceManager;

    /** 消息事件分类发，负责将用户的消息发到相应的GameChannel之中。 */
    private GameMessageDispatchServlet gameMessageDispatchServlet;

    /**
     *
     * @param gameChannelInitializer gameChannelInitializer
     * @param localServerId localServerId
     * @return void
     * @since 2024/6/26 14:52
     */
    public void start(GameChannelInitializer gameChannelInitializer, int localServerId) {
        // TODO
        // 业务处理的线程池
        GameEventExecutorGroup workerGroup = new GameEventExecutorGroup(serverConfig.getWorkerThreads());
        EventExecutorGroup rpcWorkerGroup = new DefaultEventExecutorGroup(2);

        gameMessageDispatchServlet = new GameMessageDispatchServlet(workerGroup, gameChannelInitializer);
    }

    /**
     *
     * @param context context
     * @author liuzhen
     * @since 2025/3/30 18:52
     */
    public void fireReadGameMessage(MessageContext context) {
        gameMessageDispatchServlet.fireReadGameMessage(context.getMessagePackage().getHeader().getPlayerId(), context);
    }

}
