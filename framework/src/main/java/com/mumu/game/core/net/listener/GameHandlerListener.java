/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.listener;

import org.springframework.stereotype.Component;

import com.mumu.game.core.cmd.CmdDispatch;
import com.mumu.game.core.game_netty.context.GameMessageConsumerManager;
import com.mumu.game.core.mvc.server.MessageContext;

import jakarta.annotation.Resource;

/**
 * GameHandlerListener
 *
 * @author liuzhen
 * @version 1.0.0 2025/4/1 22:32
 */
@Component
public class GameHandlerListener extends AbstractHandlerListener {

    @Resource
    CmdDispatch cmdDispatch;

    @Resource
    private GameMessageConsumerManager gameMessageConsumerManager;

    public GameHandlerListener() {
    }

    @Override
    public void handleRead(MessageContext context) {
        gameMessageConsumerManager.fireReadGameMessage(context);
    }

}