/*
 * Copyright 2020-2024, 木木996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc2.servlet.param;

import com.mumu.framework.core.mvc2.MessageContext;
import com.mumu.framework.core.mvc2.servlet.ParamHelper;

/**
 * NullInjector
 *
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:40
 */
public class NullInjector implements ParamInjector {
    private Class<?> type;

    public NullInjector(Class<?> clazz) {
        this.type = clazz;
    }

    @Override
    public Object getValue(MessageContext context) {
        return ParamHelper.getDefaultValue(type);
    }
}
