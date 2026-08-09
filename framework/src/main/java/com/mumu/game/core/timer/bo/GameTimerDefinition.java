/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.bo;

import com.mumu.game.core.timer.core.trigger.TimerTrigger;

/**
 * GameTimerDefinition
 * 可在运行期注册的游戏周期性任务定义
 * @param key 任务唯一标识
 * @param description 任务描述
 * @param task 任务执行逻辑
 * @param trigger 任务触发规则
 * @param initialDelayMillis 首次执行延迟毫秒数
 * @param maxConsecutiveFailures 最大连续失败次数，0表示不限制
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public record GameTimerDefinition(
    String key,
    String description,
    Runnable task,
    TimerTrigger trigger,
    long initialDelayMillis,
    int maxConsecutiveFailures) {

    /** 校验并标准化动态任务定义 */
    public GameTimerDefinition {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("游戏周期性任务key不能为空");
        }
        if (task == null) {
            throw new IllegalArgumentException("游戏周期性任务执行逻辑不能为空: " + key);
        }
        if (trigger == null) {
            throw new IllegalArgumentException("游戏周期性任务触发规则不能为空: " + key);
        }
        if (initialDelayMillis < 0L) {
            throw new IllegalArgumentException("游戏周期性任务首次延迟不能小于0: " + key);
        }
        if (maxConsecutiveFailures < 0) {
            throw new IllegalArgumentException("游戏周期性任务最大连续失败次数不能小于0: " + key);
        }
        key = key.trim();
        description = description == null || description.isBlank() ? key : description.trim();
    }

    /**
     * 创建使用默认策略的动态任务定义
     * @param key 任务唯一标识
     * @param task 任务执行逻辑
     * @param trigger 任务触发规则
     * @return 动态任务定义
     */
    public static GameTimerDefinition of(String key, Runnable task, TimerTrigger trigger) {
        return new GameTimerDefinition(key, key, task, trigger, 0L, 0);
    }
}
