/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core.trigger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.scheduling.support.CronExpression;

/**
 * CronTimerTrigger
 * 基于 Spring CronExpression 的触发规则
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public class CronTimerTrigger implements TimerTrigger {

    /** Cron 原始表达式 */
    private final String expression;
    /** Cron 解析对象 */
    private final CronExpression cronExpression;
    /** Cron 计算时区 */
    private final ZoneId zoneId;

    /**
     * 创建 Cron 触发规则
     * @param expression Spring 六段式 Cron 表达式
     * @param zoneId 计算时区
     */
    public CronTimerTrigger(String expression, ZoneId zoneId) {
        this.expression = expression;
        this.cronExpression = CronExpression.parse(expression);
        this.zoneId = zoneId;
    }

    @Override
    public long firstExecutionTime(long currentTime, long initialDelayMillis) {
        return calculateNext(safeAdd(currentTime, initialDelayMillis));
    }

    @Override
    public long nextExecutionTime(long completedTime) {
        return calculateNext(completedTime);
    }

    @Override
    public String expression() {
        return expression;
    }

    /**
     * 计算指定时间之后的首个 Cron 时间点
     * @param timeMillis 基准时间戳
     * @return 下次执行时间戳
     */
    private long calculateNext(long timeMillis) {
        ZonedDateTime current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zoneId);
        ZonedDateTime next = cronExpression.next(current);
        return next == null ? 0L : next.toInstant().toEpochMilli();
    }

    /**
     * 安全执行时间加法
     * @param timeMillis 时间戳
     * @param delayMillis 延迟毫秒数
     * @return 相加后的时间戳
     */
    private long safeAdd(long timeMillis, long delayMillis) {
        try {
            return Math.addExact(timeMillis, delayMillis);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Cron 首次延迟时间超出范围", e);
        }
    }
}
