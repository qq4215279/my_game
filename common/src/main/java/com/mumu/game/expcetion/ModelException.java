/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.expcetion;

/**
 * ModelException
 * 数据模型层根异常（运行时异常）
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public class ModelException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
