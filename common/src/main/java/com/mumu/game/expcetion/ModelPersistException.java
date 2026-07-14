/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.expcetion;

/**
 * ModelPersistException
 * 数据模型持久化失败（Redis / DB 等基础设施读写异常）
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public class ModelPersistException extends ModelException {
    private static final long serialVersionUID = 1L;

    public ModelPersistException(String message) {
        super(message);
    }

    public ModelPersistException(String message, Throwable cause) {
        super(message, cause);
    }
}
