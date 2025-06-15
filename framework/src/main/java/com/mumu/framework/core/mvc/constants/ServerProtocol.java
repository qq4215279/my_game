/*
 *
 *  * Copyright 2020-2025, mumu without 996.
 *  * All Right Reserved.
 *
 */

package com.mumu.framework.core.mvc.constants;

import com.mumu.framework.core.mvc.cloud.ServerInfo;
import com.mumu.framework.core.mvc.message.MessageHandlerListener;
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
