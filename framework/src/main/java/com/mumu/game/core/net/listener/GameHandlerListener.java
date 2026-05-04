/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.listener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.mumu.game.core.cmd.CmdDispatch;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;

import jakarta.annotation.Resource;

/**
 * GameHandlerListener
 *
 * @author liuzhen
 * @version 1.0.0 2025/4/1 22:32
 */
@Component
@ConditionalOnExpression("#{T(com.mumu.game.core.net.consts.ServiceType).curr() != T(com.mumu.game.core.net.consts.ServiceType).GATEWAY}")
public class GameHandlerListener extends AbstractHandlerListener {

    @Resource
    CmdDispatch cmdDispatch;

    public GameHandlerListener() {
    }

    @Override
    protected void doCommand(MessageContext context) {
        if (cmdDispatch.inSelf(context.getCmd())) {
            cmdDispatch.invokeMethod(context);
        } else {
            LogTopic.NET.warn("Not found command", "context", context);
        }
    }
}