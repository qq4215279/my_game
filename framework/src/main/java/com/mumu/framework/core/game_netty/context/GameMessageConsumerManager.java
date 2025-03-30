/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.context;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.thread.GameEventExecutorGroup;
import com.mumu.framework.core.cloud.PlayerServiceManager;
import com.mumu.framework.core.cmd.CmdDispatch;
import com.mumu.framework.core.game_netty.channel.GameChannelInitializer;
import com.mumu.framework.core.game_netty.channel.GameMessageDispatchServlet;
import com.mumu.framework.core.game_netty.channel.GameServerConfig;
import com.mumu.framework.core.log.LogTopic;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

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
    @Autowired
    private GameServerConfig serverConfig;
    /** 消息管理类，负责管理根据消息id，获取对应的消息类实例 */
    @Resource
    private CmdDispatch cmdDispatch;
    @Resource
    private PlayerServiceManager playerServiceManager;

    /** 消息事件分类发，负责将用户的消息发到相应的GameChannel之中。 */
    private GameMessageDispatchServlet gameMessageDispatchServlet;

    /**
     *
     * @param gameChannelInitializer gameChannelInitializer
     * @param localServerId localServerId
     * @return void
     * @date 2024/6/26 14:52
     */
    public void start(GameChannelInitializer gameChannelInitializer, int localServerId) {
        // 业务处理的线程池
        GameEventExecutorGroup workerGroup = new GameEventExecutorGroup(serverConfig.getWorkerThreads());
        EventExecutorGroup rpcWorkerGroup = new DefaultEventExecutorGroup(2);

        gameMessageDispatchServlet = new GameMessageDispatchServlet(workerGroup, gameChannelInitializer);
    }

    /**
     *
     * @param gameMessagePackage gameMessagePackage
     * @return void
     * @author liuzhen
     * @date 2025/3/30 18:52
     */
    public void fireReadGameMessage(GameMessagePackage gameMessagePackage) {
        gameMessageDispatchServlet.fireReadGameMessage(gameMessagePackage.getHeader().getPlayerId(), gameMessagePackage);
    }

}
