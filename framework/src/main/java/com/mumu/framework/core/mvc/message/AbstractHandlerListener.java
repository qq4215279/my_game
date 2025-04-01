/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.servlet.constants.NetConstants;

/**
 * AbstractHandlerListener
 * 抽象消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
public abstract class AbstractHandlerListener implements MessageHandlerListener {
    protected final static LogTopic log = LogTopic.ACTION;

    public AbstractHandlerListener() {
    }

    @Override
    public void handleRead(IoSession session, GameMessagePackage gameMessagePackage) {
        // TODO
        if (session.hasAttr(NetConstants.SESSION_CLIENT)) {
            innerRead();
        } else {
            outRead();
        }
    }

    // 收到内部服务器请求，处理或转发给玩家
    private void innerRead() {
        // 1. 返回给client玩家

        // 2. 转发到其他游戏服
    }

    // 接收外部玩家消息处理
    private void outRead() {
        // 1. 本服处理

        // 2. 转发给其他游戏服
    }

}
