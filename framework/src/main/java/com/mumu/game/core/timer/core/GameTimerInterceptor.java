/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

import com.mumu.game.core.timer.bo.GameTimerContext;

/**
 * GameTimerInterceptor
 * 游戏周期性任务执行拦截器
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public interface GameTimerInterceptor {

    /**
     * 任务执行前回调
     * @param context 任务运行上下文
     */
    default void beforeExecute(GameTimerContext context) {
    }

    /**
     * 任务执行后回调
     * @param context 任务运行上下文
     * @param error 任务异常，成功时为null
     */
    default void afterExecute(GameTimerContext context, Throwable error) {
    }

    /**
     * 拦截器顺序，数值越小越先执行
     * @return 排序值
     */
    default int order() {
        return 0;
    }
}
