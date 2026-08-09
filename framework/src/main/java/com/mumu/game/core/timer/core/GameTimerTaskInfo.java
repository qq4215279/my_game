/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledFuture;

import com.mumu.game.core.clock.consts.ClockType;
import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.bo.GameTimerTaskSnapshot;
import com.mumu.game.core.timer.core.trigger.TimerTrigger;

/**
 * GameTimerTaskInfo
 * 游戏周期性任务定义信息
 * @author liuzhen
 * @version 2.1.0 2026/8/9 14:58
 */
final class GameTimerTaskInfo {

    /** 任务唯一标识 */
    private final String key;
    /** 持有任务方法的 Spring Bean */
    private final Object holder;
    /** 可在当前 Bean 上调用的任务方法 */
    private final Method method;
    /** 任务来源描述 */
    private final String description;
    /** 任务触发规则 */
    private TimerTrigger trigger;
    /** 首次执行延迟毫秒数 */
    private final long initialDelayMillis;
    /** 最大连续失败次数，0表示不限制 */
    private final int maxConsecutiveFailures;
    /** 任务使用的时间类型 */
    private final ClockType clockType;
    /** 任务可变运行状态 */
    private final GameTimerRuntimeState runtimeState = new GameTimerRuntimeState();

    /**
     * 创建任务信息
     * @param key 任务唯一标识
     * @param holder 持有任务方法的 Spring Bean
     * @param method 任务方法
     * @param description 任务来源描述
     * @param trigger 任务触发规则
     * @param initialDelayMillis 首次执行延迟毫秒数
     * @param maxConsecutiveFailures 最大连续失败次数
     * @param clockType 任务使用的时间类型
     */
    GameTimerTaskInfo(String key, Object holder, Method method, String description, TimerTrigger trigger,
        long initialDelayMillis, int maxConsecutiveFailures, ClockType clockType) {
        this.key = key;
        this.holder = holder;
        this.method = method;
        this.description = description;
        this.trigger = trigger;
        this.initialDelayMillis = initialDelayMillis;
        this.maxConsecutiveFailures = maxConsecutiveFailures;
        this.clockType = clockType;
    }

    /**
     * 准备进入等待调度状态
     * @param nextTime 下次执行时间戳
     * @param fromPaused 是否从暂停状态发起调度
     * @return true表示状态切换成功
     */
    synchronized boolean prepareSchedule(long nextTime, boolean fromPaused) {
        return runtimeState.prepareSchedule(nextTime, fromPaused);
    }

    /**
     * 准备进入等待调度状态
     * @param nextTime 下次执行时间戳
     * @param fromPaused 是否从暂停状态发起调度
     * @param resetFailureState 是否重置失败状态
     * @return true表示状态切换成功
     */
    synchronized boolean prepareSchedule(long nextTime, boolean fromPaused, boolean resetFailureState) {
        return runtimeState.prepareSchedule(nextTime, fromPaused, resetFailureState);
    }

    /**
     * 绑定调度句柄
     * @param scheduledFuture 调度句柄
     */
    synchronized void bindFuture(ScheduledFuture<?> scheduledFuture) {
        runtimeState.bindFuture(scheduledFuture);
    }

    /** 调度提交失败后恢复为暂停状态 */
    synchronized void scheduleFailed() {
        runtimeState.scheduleFailed();
    }

    /**
     * 开始执行正常到期的任务
     * @return true表示允许执行
     */
    synchronized boolean beginScheduledExecution(long currentTime) {
        return runtimeState.beginScheduledExecution(currentTime);
    }

    /**
     * 开始手动执行任务
     * @return true表示允许执行
     */
    synchronized boolean beginManualExecution(long currentTime) {
        return runtimeState.beginManualExecution(currentTime);
    }

    /**
     * 开始执行游戏时间跨越触发点后的单次补偿任务
     * @param currentTime 当前时间戳
     * @return true表示允许执行
     */
    synchronized boolean beginFireOnceExecution(long currentTime) {
        return runtimeState.beginFireOnceExecution(currentTime);
    }

    /**
     * 记录任务执行结果
     * @param error 执行异常，成功时为null
     * @return true表示执行结束后需要继续调度
     */
    synchronized boolean completeExecution(Throwable error, long currentTime) {
        return runtimeState.completeExecution(error, maxConsecutiveFailures, currentTime);
    }

    /**
     * 暂停任务
     * @return true表示暂停成功
     */
    synchronized boolean pause() {
        return runtimeState.pause("手动暂停");
    }

    /**
     * 使用指定原因暂停任务
     * @param reason 暂停原因
     * @return true表示暂停成功
     */
    synchronized boolean pause(String reason) {
        return runtimeState.pause(reason);
    }

    /**
     * 为已经暂停的任务记录暂停原因
     * @param reason 暂停原因
     */
    synchronized void recordPausedReason(String reason) {
        runtimeState.recordPausedReason(reason);
    }

    /** 永久停止当前任务 */
    synchronized void stop() {
        runtimeState.stop();
    }

    /**
     * 生成只读任务快照
     * @return 任务运行快照
     */
    synchronized GameTimerTaskSnapshot snapshot() {
        return runtimeState.snapshot(key, description, trigger.expression(), clockType, maxConsecutiveFailures);
    }

    /**
     * 生成任务运行上下文
     * @param currentTime 当前时间戳
     * @return 任务运行上下文
     */
    synchronized GameTimerContext context(long currentTime) {
        return runtimeState.context(key, currentTime);
    }

    /**
     * 判断当前状态是否允许更新触发规则
     * @return true表示允许更新
     */
    synchronized boolean canUpdateTrigger() {
        return runtimeState.canUpdateTrigger();
    }

    /**
     * 判断任务是否处于等待调度状态
     * @return true表示正在等待调度
     */
    synchronized boolean isScheduled() {
        return runtimeState.isScheduled();
    }

    /**
     * 准备因游戏时间变化而重新调度
     * @return true表示原任务处于等待调度状态
     */
    synchronized boolean prepareClockReschedule() {
        return runtimeState.prepareClockReschedule();
    }

    /**
     * 获取下次计划执行时间
     * @return 下次计划执行时间戳
     */
    synchronized long getNextExecutionTime() {
        return runtimeState.getNextExecutionTime();
    }

    /**
     * 获取上次已经执行的计划时间
     * @return 上次已经执行的计划时间戳
     */
    synchronized long getLastExecutedScheduledTime() {
        return runtimeState.getLastExecutedScheduledTime();
    }

    /**
     * 替换触发规则并进入暂停状态
     * @param newTrigger 新触发规则
     * @return 原触发规则
     */
    synchronized TimerTrigger replaceTrigger(TimerTrigger newTrigger) {
        TimerTrigger oldTrigger = trigger;
        runtimeState.prepareTriggerReplacement();
        trigger = newTrigger;
        return oldTrigger;
    }

    /**
     * 恢复原触发规则
     * @param oldTrigger 原触发规则
     */
    synchronized void restoreTrigger(TimerTrigger oldTrigger) {
        trigger = oldTrigger;
    }

    /** 确认触发规则更新成功 */
    synchronized void confirmTriggerUpdate() {
        runtimeState.confirmTriggerUpdate();
    }

    String getKey() {
        return key;
    }

    Object getHolder() {
        return holder;
    }

    Method getMethod() {
        return method;
    }

    synchronized TimerTrigger getTrigger() {
        return trigger;
    }

    long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    ClockType getClockType() {
        return clockType;
    }
}
