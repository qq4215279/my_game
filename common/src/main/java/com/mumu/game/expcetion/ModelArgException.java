/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.expcetion;

/**
 * ModelArgException
 * 数据模型参数/用法错误（索引键不完整、索引不存在、路由键为空等）
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public class ModelArgException extends ModelException {
    private static final long serialVersionUID = 1L;

    public ModelArgException(String message) {
        super(message);
    }

    public ModelArgException(String message, Throwable cause) {
        super(message, cause);
    }
}
