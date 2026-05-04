/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.listener;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.thread.ThreadPoolRouter;

import jakarta.annotation.Resource;

/**
 * AbstractHandlerListener
 * 抽象消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
public abstract class AbstractHandlerListener implements MessageHandlerListener {
    protected final static LogTopic log = LogTopic.ACTION;

    @Resource
    ThreadPoolRouter threadPoolRouter;

    public AbstractHandlerListener() {
    }

    @Override
    public final void handleRead(MessageContext context) {
        threadPoolRouter.autoExecute(context, () -> doCommand(context));
    }

    /** 处理消息(已经切换到逻辑线程处理了) */
    protected abstract void doCommand(MessageContext context);
}
