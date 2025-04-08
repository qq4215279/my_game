/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd.param.injector;

import com.mumu.framework.core.mvc.server.MessageContext;

/**
 * ArrayInjector
 *
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
