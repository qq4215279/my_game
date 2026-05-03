/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.game_netty.channel;

/**
 * GameChannelInitializer
 * 游戏ChannelInitializer
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:57
 */
public interface GameChannelInitializer {

    /**
     *
     * @param channel channel
     */
    void initChannel(GameChannel channel);
}
