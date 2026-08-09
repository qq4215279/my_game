/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.mumu.game.core.clock.consts.ClockType;
import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.bo.GameTimerTaskSnapshot;
import com.mumu.game.core.timer.consts.GameTimerState;

/**
 * GameTimerRuntimeState
 * 游戏周期性任务可变运行状态，由GameTimerTaskInfo统一加锁后访问
 * @author liuzhen
 * @version 1.0.0 2026/8/9 14:58
 */
final class GameTimerRuntimeState {

    /** 当前任务状态 */
    private GameTimerState state = GameTimerState.PAUSED;
    /** 当前等待执行的调度句柄 */
    private ScheduledFuture<?> future;
    /** 当前执行结束后是否继续调度 */
    private boolean rescheduleAfterExecution;
    /** 下次计划执行时间 */
    private long nextExecutionTime;
    /** 上次计划执行时间 */
    private long lastScheduledTime;
    /** 上次已经执行的计划时间 */
    private long lastExecutedScheduledTime;
    /** 上次实际开始时间 */
    private long lastStartTime;
    /** 本次开始执行时的单调时间 */
    private long executionStartNanoTime;
    /** 上次实际完成时间 */
    private long lastCompleteTime;
    /** 上次执行耗时 */
    private long lastExecutionDuration;
    /** 最大执行耗时 */
    private long maxExecutionDuration;
    /** 上次实际触发延迟 */
    private long lastExecutionDelay;
    /** 总执行次数 */
    private long executionCount;
    /** 手动执行次数 */
    private long manualExecutionCount;
    /** 成功次数 */
    private long successCount;
    /** 失败次数 */
    private long failureCount;
    /** 连续失败次数 */
    private int consecutiveFailureCount;
    /** 触发规则版本 */
    private long triggerVersion = 1L;
    /** 暂停原因 */
    private String pauseReason = "";
    /** 上一次异常摘要 */
    private String lastError = "";

    /**
     * 准备进入等待调度状态
     * @param nextTime 下次执行时间戳
     * @param fromPaused 是否从暂停状态发起调度
     * @return true表示状态切换成功
     */
    boolean prepareSchedule(long nextTime, boolean fromPaused) {
        return prepareSchedule(nextTime, fromPaused, true);
    }

    /**
     * 准备进入等待调度状态
     * @param nextTime 下次执行时间戳
     * @param fromPaused 是否从暂停状态发起调度
     * @param resetFailureState 是否重置失败状态
     * @return true表示状态切换成功
     */
    boolean prepareSchedule(long nextTime, boolean fromPaused, boolean resetFailureState) {
        if (state == GameTimerState.STOPPED || nextTime <= 0L) {
            return false;
        }
        if (fromPaused && state != GameTimerState.PAUSED) {
            return false;
        }
        if (!fromPaused && state != GameTimerState.RUNNING) {
            return false;
        }
        state = GameTimerState.SCHEDULED;
        nextExecutionTime = nextTime;
        lastScheduledTime = nextTime;
        future = null;
        if (fromPaused && resetFailureState) {
            consecutiveFailureCount = 0;
            pauseReason = "";
        }
        return true;
    }

    /**
     * 绑定调度句柄
     * @param scheduledFuture 调度句柄
     */
    void bindFuture(ScheduledFuture<?> scheduledFuture) {
        if (state == GameTimerState.SCHEDULED) {
            future = scheduledFuture;
            return;
        }
        scheduledFuture.cancel(false);
    }

    /** 调度提交失败后恢复为暂停状态 */
    void scheduleFailed() {
        if (state != GameTimerState.STOPPED) {
            state = GameTimerState.PAUSED;
            pauseReason = "调度提交失败";
        }
        future = null;
        nextExecutionTime = 0L;
        rescheduleAfterExecution = false;
    }

    /**
     * 开始执行正常到期的任务
     * @return true表示允许执行
     */
    boolean beginScheduledExecution(long currentTime) {
        if (state != GameTimerState.SCHEDULED) {
            return false;
        }
        lastExecutedScheduledTime = lastScheduledTime;
        future = null;
        nextExecutionTime = 0L;
        state = GameTimerState.RUNNING;
        rescheduleAfterExecution = true;
        recordExecutionStart(false, currentTime);
        return true;
    }

    /**
     * 开始手动执行任务
     * @return true表示允许执行
     */
    boolean beginManualExecution(long currentTime) {
        if (state == GameTimerState.RUNNING || state == GameTimerState.STOPPED) {
            return false;
        }
        boolean wasScheduled = state == GameTimerState.SCHEDULED;
        cancelFuture();
        state = GameTimerState.RUNNING;
        rescheduleAfterExecution = wasScheduled;
        recordExecutionStart(true, currentTime);
        return true;
    }

    /**
     * 开始执行游戏时间跨越触发点后的单次补偿任务
     * @param currentTime 当前时间戳
     * @return true表示允许执行
     */
    boolean beginFireOnceExecution(long currentTime) {
        if (state != GameTimerState.SCHEDULED) {
            return false;
        }
        lastExecutedScheduledTime = nextExecutionTime;
        cancelFuture();
        state = GameTimerState.RUNNING;
        rescheduleAfterExecution = true;
        recordExecutionStart(false, currentTime);
        return true;
    }

    /**
     * 记录任务执行结果
     * @param error 执行异常，成功时为null
     * @param maxConsecutiveFailures 最大连续失败次数
     * @return true表示执行结束后需要继续调度
     */
    boolean completeExecution(Throwable error, int maxConsecutiveFailures, long currentTime) {
        lastCompleteTime = currentTime;
        lastExecutionDuration = Math.max(
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - executionStartNanoTime), 0L);
        maxExecutionDuration = Math.max(maxExecutionDuration, lastExecutionDuration);
        if (error == null) {
            successCount++;
            consecutiveFailureCount = 0;
            lastError = "";
        } else {
            failureCount++;
            consecutiveFailureCount++;
            lastError = buildErrorSummary(error);
        }

        boolean shouldReschedule = state == GameTimerState.RUNNING && rescheduleAfterExecution;
        if (shouldReschedule && maxConsecutiveFailures > 0
            && consecutiveFailureCount >= maxConsecutiveFailures) {
            shouldReschedule = false;
            state = GameTimerState.PAUSED;
            pauseReason = "连续失败达到阈值:" + maxConsecutiveFailures;
        }
        if (state == GameTimerState.RUNNING && !shouldReschedule) {
            state = GameTimerState.PAUSED;
        }
        rescheduleAfterExecution = false;
        return shouldReschedule;
    }

    /**
     * 暂停任务
     * @param reason 暂停原因
     * @return true表示暂停成功
     */
    boolean pause(String reason) {
        if (state == GameTimerState.PAUSED || state == GameTimerState.STOPPED) {
            return false;
        }
        cancelFuture();
        state = GameTimerState.PAUSED;
        rescheduleAfterExecution = false;
        pauseReason = reason == null ? "" : reason;
        return true;
    }

    /**
     * 为已经暂停的任务记录暂停原因
     * @param reason 暂停原因
     */
    void recordPausedReason(String reason) {
        if (state == GameTimerState.PAUSED) {
            pauseReason = reason == null ? "" : reason;
        }
    }

    /** 永久停止当前任务 */
    void stop() {
        cancelFuture();
        state = GameTimerState.STOPPED;
        rescheduleAfterExecution = false;
        pauseReason = "任务已停止";
    }

    /** 准备替换任务触发规则 */
    void prepareTriggerReplacement() {
        cancelFuture();
        state = GameTimerState.PAUSED;
    }

    /**
     * 准备因游戏时间变化而重新调度
     * @return true表示原任务处于等待调度状态
     */
    boolean prepareClockReschedule() {
        if (state != GameTimerState.SCHEDULED) {
            return false;
        }
        cancelFuture();
        state = GameTimerState.PAUSED;
        return true;
    }

    /** 触发规则更新成功 */
    void confirmTriggerUpdate() {
        triggerVersion++;
    }

    /**
     * 生成任务运行上下文
     * @param key 任务唯一标识
     * @param currentTime 当前时间戳
     * @return 任务运行上下文
     */
    GameTimerContext context(String key, long currentTime) {
        return new GameTimerContext(key, currentTime, lastScheduledTime, lastStartTime, lastCompleteTime,
            executionCount, failureCount, consecutiveFailureCount);
    }

    /**
     * 生成只读任务快照
     * @param key 任务唯一标识
     * @param description 任务来源描述
     * @param expression 触发规则描述
     * @param maxConsecutiveFailures 最大连续失败次数
     * @return 任务运行快照
     */
    GameTimerTaskSnapshot snapshot(
        String key,
        String description,
        String expression,
        ClockType clockType,
        int maxConsecutiveFailures) {
        return new GameTimerTaskSnapshot(key, description, expression, clockType, state, nextExecutionTime,
            lastScheduledTime, lastExecutedScheduledTime, lastStartTime, lastCompleteTime, lastExecutionDuration,
            maxExecutionDuration, lastExecutionDelay, executionCount, manualExecutionCount, successCount,
            failureCount, consecutiveFailureCount, maxConsecutiveFailures, triggerVersion, pauseReason, lastError);
    }

    /**
     * 判断当前状态是否允许更新触发规则
     * @return true表示允许更新
     */
    boolean canUpdateTrigger() {
        return state != GameTimerState.RUNNING && state != GameTimerState.STOPPED;
    }

    /**
     * 判断任务是否处于等待调度状态
     * @return true表示正在等待调度
     */
    boolean isScheduled() {
        return state == GameTimerState.SCHEDULED;
    }

    /**
     * 获取下次计划执行时间
     * @return 下次计划执行时间戳
     */
    long getNextExecutionTime() {
        return nextExecutionTime;
    }

    /**
     * 获取上次已经执行的计划时间
     * @return 上次已经执行的计划时间戳
     */
    long getLastExecutedScheduledTime() {
        return lastExecutedScheduledTime;
    }

    /**
     * 记录任务开始执行
     * @param manual 是否为手动执行
     */
    private void recordExecutionStart(boolean manual, long currentTime) {
        lastStartTime = currentTime;
        executionStartNanoTime = System.nanoTime();
        lastExecutionDelay = manual || lastScheduledTime <= 0L ? 0L
            : Math.max(lastStartTime - lastScheduledTime, 0L);
        executionCount++;
        if (manual) {
            manualExecutionCount++;
        }
    }

    /** 取消当前等待执行的任务 */
    private void cancelFuture() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        nextExecutionTime = 0L;
    }

    /**
     * 构建异常摘要
     * @param error 执行异常
     * @return 异常摘要
     */
    private String buildErrorSummary(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
