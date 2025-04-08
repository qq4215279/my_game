/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.framework.core.mvc.server.IoSession;
import com.mumu.framework.core.mvc.server.MessageContext;

import io.netty.channel.ChannelHandlerContext;

/**
 * MessageHandlerListener
 * 消息处理监听器，用于监听消息处理的各个阶段
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:31
 */
public interface MessageHandlerListener {
    /** 服务器启动 */
    default void handleStart() {}

    /** 连接打开 */
    default void handleActive(IoSession session) {}

    /** 获取消息 */
    default void handleRead(MessageContext context) {}

    /** 事件传播处理（注：当无需处理时，调用super.userEventTriggered进行传播即可） */
    default void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
    }

    /** 连接关闭 */
    default void handlerRemoved(IoSession session) {}

    /** 默认的消息处理监听器 */
    MessageHandlerListener DUMMY = new MessageHandlerListener() {};
}
