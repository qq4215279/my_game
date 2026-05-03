/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.mvc.servlet;

import io.netty.channel.ChannelHandlerContext;

/**
 * Servlet
 * 处理器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:04
 */
public interface Servlet {

    /**
     * 初始化
     */
    void init();

    /**
     * 处理请求
     */
    void doCommand(ChannelHandlerContext ctx, Object msg);

    /**
     *
     */
    void destroy();
}
