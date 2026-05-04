/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.param.injector;

import com.mumu.game.core.net.server.MessageContext;

/**
 * ArrayInjector
 * 数组注入器
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:37
 */
public class ArrayInjector implements ParamInjector {
    protected String name;
    protected Class<?> type;

    public ArrayInjector(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public Object getValue(MessageContext context) {
        return null;
    }
}
