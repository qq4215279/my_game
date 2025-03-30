/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.context;

import com.mumu.framework.core.cmd.response.ResponseResult;

/**
 * GameMessageContext
 * 游戏消息上下文
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:43
 */
public interface GameMessageContext {
    long getPlayerId();

    String getRemoteHost();

    <T> T getRequest();

    void sendMessage(ResponseResult message);
}
