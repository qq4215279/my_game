/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core.trigger;

/**
 * FixedDelayTimerTrigger
 * 固定延迟触发规则，下一次任务在上一次任务完成后开始计时
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public class FixedDelayTimerTrigger implements TimerTrigger {

    /** 两次任务之间的固定延迟毫秒数 */
    private final long delayMillis;

    /**
     * 创建固定延迟触发规则
     * @param delayMillis 固定延迟毫秒数
     */
    public FixedDelayTimerTrigger(long delayMillis) {
        if (delayMillis <= 0L) {
            throw new IllegalArgumentException("固定延迟必须大于0毫秒");
        }
        this.delayMillis = delayMillis;
    }

    @Override
    public long firstExecutionTime(long currentTime, long initialDelayMillis) {
        return safeAdd(currentTime, initialDelayMillis);
    }

    @Override
    public long nextExecutionTime(long completedTime) {
        return safeAdd(completedTime, delayMillis);
    }

    @Override
    public String expression() {
        return "fixedDelay:" + delayMillis + "ms";
    }

    /**
     * 安全执行时间加法
     * @param timeMillis 时间戳
     * @param delay 延迟毫秒数
     * @return 相加后的时间戳
     */
    private long safeAdd(long timeMillis, long delay) {
        try {
            return Math.addExact(timeMillis, delay);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("固定延迟时间超出范围", e);
        }
    }
}
