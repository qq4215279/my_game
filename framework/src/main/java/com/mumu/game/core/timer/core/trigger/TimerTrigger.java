/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core.trigger;

import com.mumu.game.core.timer.bo.GameTimerContext;

/**
 * TimerTrigger
 * 周期性任务触发规则
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public interface TimerTrigger {

    /**
     * 计算首次执行时间
     * @param currentTime 当前时间戳
     * @param initialDelayMillis 首次延迟毫秒数
     * @return 首次执行时间戳，0表示没有后续执行时间
     */
    long firstExecutionTime(long currentTime, long initialDelayMillis);

    /**
     * 计算下一次执行时间
     * @param completedTime 上一次任务完成时间戳
     * @return 下一次执行时间戳，0表示没有后续执行时间
     */
    long nextExecutionTime(long completedTime);

    /**
     * 根据完整任务上下文计算首次执行时间
     * @param context 任务运行上下文
     * @param initialDelayMillis 首次延迟毫秒数
     * @return 首次执行时间戳
     */
    default long firstExecutionTime(GameTimerContext context, long initialDelayMillis) {
        return firstExecutionTime(context.currentTime(), initialDelayMillis);
    }

    /**
     * 根据完整任务上下文计算下一次执行时间
     * @param context 任务运行上下文
     * @return 下一次执行时间戳
     */
    default long nextExecutionTime(GameTimerContext context) {
        return nextExecutionTime(context.currentTime());
    }

    /**
     * 获取触发规则描述
     * @return 触发规则描述
     */
    String expression();
}
