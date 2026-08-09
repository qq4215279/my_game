/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.bo;

import com.mumu.game.core.clock.consts.ClockType;
import com.mumu.game.core.timer.consts.GameTimerState;

/**
 * GameTimerTaskSnapshot
 * 游戏周期性任务只读运行快照
 * @param key 任务唯一标识
 * @param method 任务方法
 * @param expression 触发规则
 * @param clockType 任务使用的时间类型
 * @param state 当前状态
 * @param nextExecutionTime 下次执行时间戳
 * @param lastScheduledTime 上次计划执行时间戳
 * @param lastExecutedScheduledTime 上次已经执行的计划时间戳
 * @param lastStartTime 上次开始时间戳
 * @param lastCompleteTime 上次完成时间戳
 * @param lastExecutionDuration 上次执行耗时
 * @param maxExecutionDuration 最大执行耗时
 * @param lastExecutionDelay 上次实际触发延迟
 * @param executionCount 总执行次数
 * @param manualExecutionCount 手动执行次数
 * @param successCount 成功次数
 * @param failureCount 失败次数
 * @param consecutiveFailureCount 连续失败次数
 * @param maxConsecutiveFailures 最大连续失败次数
 * @param triggerVersion 触发规则版本
 * @param pauseReason 暂停原因
 * @param lastError 上一次异常摘要
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public record GameTimerTaskSnapshot(
    String key,
    String method,
    String expression,
    ClockType clockType,
    GameTimerState state,
    long nextExecutionTime,
    long lastScheduledTime,
    long lastExecutedScheduledTime,
    long lastStartTime,
    long lastCompleteTime,
    long lastExecutionDuration,
    long maxExecutionDuration,
    long lastExecutionDelay,
    long executionCount,
    long manualExecutionCount,
    long successCount,
    long failureCount,
    int consecutiveFailureCount,
    int maxConsecutiveFailures,
    long triggerVersion,
    String pauseReason,
    String lastError) {
}
