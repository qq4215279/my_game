/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * GameServerConfig
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:21
 */
@Configuration
@ConfigurationProperties(prefix = "game.server.config")
@Data
public class GameServerConfig {
    /** 游戏服务id */
    private int serviceId;
    /** 游戏服务所在的服务器id */
    private int serverId;
    /** 业务处理线程数 */
    private int workerThreads = 4;
    /** db处理线程数 */
    private int dbThreads = 16;
    private int flushRedisDelaySecond = 60;
    private int flushDBDelaySeond = 300;

}
