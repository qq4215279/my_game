/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core.trigger;

import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.core.NextExecutionTimeProvider;

/**
 * DynamicTimerTrigger
 * 由业务提供者动态计算下一次执行时间
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public class DynamicTimerTrigger implements TimerTrigger {

    /** 动态时间提供者 */
    private final NextExecutionTimeProvider provider;
    /** 触发规则描述 */
    private final String description;

    /**
     * 创建动态触发规则
     * @param provider 动态时间提供者
     * @param description 触发规则描述
     */
    public DynamicTimerTrigger(NextExecutionTimeProvider provider, String description) {
        if (provider == null) {
            throw new IllegalArgumentException("动态时间提供者不能为空");
        }
        this.provider = provider;
        this.description = description == null || description.isBlank() ? provider.getClass().getName() : description;
    }

    @Override
    public long firstExecutionTime(long currentTime, long initialDelayMillis) {
        GameTimerContext context = new GameTimerContext("", currentTime, 0L, 0L, 0L, 0L, 0L, 0);
        return firstExecutionTime(context, initialDelayMillis);
    }

    @Override
    public long nextExecutionTime(long completedTime) {
        GameTimerContext context = new GameTimerContext("", completedTime, 0L, 0L, completedTime, 0L, 0L, 0);
        return nextExecutionTime(context);
    }

    @Override
    public long firstExecutionTime(GameTimerContext context, long initialDelayMillis) {
        long calculateTime;
        try {
            calculateTime = Math.addExact(context.currentTime(), initialDelayMillis);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("动态任务首次延迟时间超出范围", e);
        }
        return normalize(provider.nextExecutionTime(context.atTime(calculateTime)), calculateTime);
    }

    @Override
    public long nextExecutionTime(GameTimerContext context) {
        return normalize(provider.nextExecutionTime(context), context.currentTime());
    }

    @Override
    public String expression() {
        return "dynamic:" + description;
    }

    /**
     * 标准化业务返回的下次执行时间
     * @param nextTime 业务返回的时间戳
     * @param currentTime 当前计算时间戳
     * @return 合法执行时间，0表示停止调度
     */
    private long normalize(long nextTime, long currentTime) {
        return nextTime > currentTime ? nextTime : 0L;
    }
}
