/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.bo;

/**
 * GameTimerContext
 * 游戏周期性任务运行上下文
 * @param key 任务唯一标识
 * @param currentTime 当前计算时间戳
 * @param scheduledTime 本次计划执行时间戳
 * @param lastStartTime 上次开始时间戳
 * @param lastCompleteTime 上次完成时间戳
 * @param executionCount 总执行次数
 * @param failureCount 总失败次数
 * @param consecutiveFailureCount 连续失败次数
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public record GameTimerContext(
    String key,
    long currentTime,
    long scheduledTime,
    long lastStartTime,
    long lastCompleteTime,
    long executionCount,
    long failureCount,
    int consecutiveFailureCount) {

    /**
     * 使用新的当前时间创建上下文
     * @param timeMillis 当前时间戳
     * @return 新任务上下文
     */
    public GameTimerContext atTime(long timeMillis) {
        return new GameTimerContext(key, timeMillis, scheduledTime, lastStartTime, lastCompleteTime, executionCount,
            failureCount, consecutiveFailureCount);
    }
}
