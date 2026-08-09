/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CronUtil
 * Cron表达式工具类
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
public final class CronUtil {

    /** 工具类禁止实例化 */
    private CronUtil() {
    }

    /**
     * 获取最近一次执行时间（Spring框架提供）
     * @param cron Cron表达式
     * @return java.time.LocalDateTime
     */
    public static LocalDateTime getNextTime(String cron) {
        return CollUtil.getFirst(getNextTimes(cron, 1, LocalDateTime.now()));
    }

    /**
     * 获取 cron表达式的后count次执行时间（Spring框架提供）
     * @param cron Cron表达式
     * @param count 次数
     * @return java.util.List<java.time.LocalDateTime>
     */
    public static List<LocalDateTime> getNextTimes(String cron, int count) {
        return getNextTimes(cron, count, LocalDateTime.now());
    }

    /**
     * 获取 cron表 从start开始的后count次执行时间（Spring框架提供）
     * @param cron Cron表达式
     * @param count 次数
     * @param start 开始时间
     * @return java.util.List<java.time.LocalDateTime>
     */
    public static List<LocalDateTime> getNextTimes(String cron, int count, LocalDateTime start) {
        checkArguments(cron, count, start);
        List<LocalDateTime> times = new ArrayList<>(count);
        CronExpression cronExpression = CronExpression.parse(cron.trim());

        LocalDateTime curr = start;
        for (int i = 0; i < count; i++) {
            curr = cronExpression.next(curr);
            if (curr == null) break;
            times.add(curr);
        }
        return times;
    }

    /**
     * 获取指定时区的最近一次执行时间
     * @param cron Cron表达式
     * @param zoneId 时区
     * @return 最近一次执行时间
     */
    public static ZonedDateTime getNextTime(String cron, ZoneId zoneId) {
        List<ZonedDateTime> times = getNextTimes(cron, 1, ZonedDateTime.now(zoneId));
        return times.isEmpty() ? null : times.getFirst();
    }

    /**
     * 获取指定时区的后count次执行时间
     * @param cron Cron表达式
     * @param count 次数
     * @param start 开始时间
     * @return 后续执行时间列表
     */
    public static List<ZonedDateTime> getNextTimes(String cron, int count, ZonedDateTime start) {
        checkArguments(cron, count, start);
        List<ZonedDateTime> times = new ArrayList<>(count);
        CronExpression cronExpression = CronExpression.parse(cron.trim());

        ZonedDateTime current = start;
        for (int i = 0; i < count; i++) {
            current = cronExpression.next(current);
            if (current == null) {
                break;
            }
            times.add(current);
        }
        return times;
    }

    /**
     * 校验Spring六段式Cron表达式是否合法
     * @param expression Cron表达式
     * @return true表示表达式合法
     */
    public static boolean isValid(String expression) {
        return expression != null && !expression.isBlank()
            && CronExpression.isValidExpression(expression.trim());
    }

    /**
     * 校验公共计算参数
     * @param cron Cron表达式
     * @param count 次数
     * @param start 开始时间
     */
    private static void checkArguments(String cron, int count, Object start) {
        if (!isValid(cron)) {
            throw new IllegalArgumentException("Cron表达式不合法: " + cron);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Cron执行次数不能小于0");
        }
        if (start == null) {
            throw new IllegalArgumentException("Cron开始时间不能为空");
        }
    }

    /** Hutool 提供的cron解析工具（此方法不支持复杂表达式解析，如 0 30 18 ? * 6L，L无法解析，建议用上面 spring的） */
    public static List<Date> matchedDates(String cron, int count, Date start) {
        return CronPatternUtil.matchedDates(cron, start, count, true);
    }
}
