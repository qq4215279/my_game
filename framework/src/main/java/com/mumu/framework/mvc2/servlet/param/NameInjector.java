/*
 * Copyright 2020-2024, 木木996.
 * All Right Reserved.
 */

package com.mumu.framework.mvc2.servlet.param;

import com.mumu.framework.mvc2.MessageContext;
import com.mumu.framework.mvc2.servlet.ParamHelper;

/**
 * NameInjector
 *
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:49
 */
public class NameInjector implements ParamInjector {
    protected String name;
    protected Class<?> type;

    public NameInjector(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public Object getValue(MessageContext context) {
        // TODO
        String[] params = null;
        return ParamHelper.castTo(params, type);
    }
}
