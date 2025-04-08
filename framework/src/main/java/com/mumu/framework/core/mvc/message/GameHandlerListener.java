/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import org.springframework.stereotype.Component;

import com.mumu.framework.core.cmd.CmdDispatch;
import com.mumu.framework.core.game_netty.context.GameMessageConsumerManager;
import com.mumu.framework.core.mvc.server.MessageContext;

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