/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.param.injector;

import com.mumu.game.core.net.server.MessageContext;

/**
 * ParamInjector
 * 参数注入
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:35
 */
public interface ParamInjector {

    /**
     * 获取值
     * @return java.lang.Object
     * @since 2024/12/5 22:36
     */
    Object getValue(MessageContext context);
}
