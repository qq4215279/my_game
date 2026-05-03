/*
 *
 *  * Copyright 2020-2026, mumu without 996.
 *  * All Right Reserved.
 *
 */

package com.mumu.game.core.net.consts;

import com.mumu.game.core.net.channel.HttpChannelInitializer;
import com.mumu.game.core.net.channel.SocketChannelInitializer;
import com.mumu.game.core.net.channel.WebSocketChannelInitializer;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;

import io.netty.channel.ChannelInitializer;

/**
 * ServerProtocol
 * 注册服务端协议类型
 * @author liuzhen
 * @version 1.0.0 2025/6/15 17:06
 */
@SuppressWarnings("rawtypes")
public enum ServerProtocol {
    SOCKET {
        @Override
        public ChannelInitializer getChannelInitializer(
                MessageHandlerListener listener, ServerInfo serverInfo) {
            return new SocketChannelInitializer(listener, serverInfo);
        }
    },
    WEBSOCKET {
        @Override
        public ChannelInitializer getChannelInitializer(
                MessageHandlerListener listener, ServerInfo serverInfo) {
            return new WebSocketChannelInitializer(listener, serverInfo);
        }
    },
    HTTP {
        @Override
        public ChannelInitializer getChannelInitializer(
                MessageHandlerListener listener, ServerInfo serverInfo) {
            return new HttpChannelInitializer(listener, serverInfo);
        }
    };

    /** 获取 channel 初始化类 */
    public abstract ChannelInitializer getChannelInitializer(
            MessageHandlerListener listener, ServerInfo serverInfo);
}
