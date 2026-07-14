/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.expcetion;

/**
 * ModelStateException
 * 数据模型状态错误（引用不一致、记录已存在/不存在、缓存键非法等）
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public class ModelStateException extends ModelException {
    private static final long serialVersionUID = 1L;

    public ModelStateException(String message) {
        super(message);
    }

    public ModelStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
