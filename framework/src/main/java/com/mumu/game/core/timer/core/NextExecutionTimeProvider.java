/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

import com.mumu.game.core.timer.bo.GameTimerContext;

/**
 * NextExecutionTimeProvider
 * 动态计算任务下一次执行时间
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
@FunctionalInterface
public interface NextExecutionTimeProvider {

    /**
     * 计算下一次执行时间
     * @param context 任务运行上下文
     * @return 下一次执行时间戳，返回小于等于当前时间的值表示不再调度
     */
    long nextExecutionTime(GameTimerContext context);

    /**
     * None
     * 注解未配置动态时间提供者时使用的占位类型
     * @author liuzhen
     * @version 2.0.0 2026/8/9 12:19
     */
    final class None implements NextExecutionTimeProvider {

        @Override
        public long nextExecutionTime(GameTimerContext context) {
            return 0L;
        }
    }
}
