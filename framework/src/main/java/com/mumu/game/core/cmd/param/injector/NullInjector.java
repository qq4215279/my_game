/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.param.injector;

import com.mumu.game.core.cmd.param.ParamHelper;
import com.mumu.game.core.net.server.MessageContext;

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
