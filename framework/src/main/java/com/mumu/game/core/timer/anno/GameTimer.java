/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import com.mumu.game.core.clock.consts.ClockType;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.timer.core.NextExecutionTimeProvider;
import org.springframework.stereotype.Component;

/**
 * GameTimer
 * 游戏周期性任务注解
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
@Documented
@Inherited
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GameTimer {

    /** 任务唯一标识，同一个服务进程内不可重复 */
    String key();

    /** 执行时间配置-方式1: Spring 六段式 Cron 表达式。注：与 cron、fixedDelay、nextTimeProvider 三选一 */
    String cron() default "";
    /** 执行时间配置-方式2: 固定延迟时间。注：与 cron、fixedDelay、nextTimeProvider 三选一 */
    long fixedDelay() default -1L;
    /** 执行时间配置-方式3: 动态时间提供者类型，必须是 Spring Bean。注：与 cron、fixedDelay、nextTimeProvider 三选一 */
    Class<? extends NextExecutionTimeProvider> nextTimeProvider() default NextExecutionTimeProvider.None.class;

    /** 首次执行前的延迟时间 */
    long initialDelay() default 0L;
    /** fixedDelay 和 initialDelay 的时间单位（默认毫秒值） */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /** Cron 时区，空字符串表示使用系统默认时区 */
    String zone() default "";

    /** 任务使用的时间类型，固定延迟任务始终按真实流逝时间执行 */
    ClockType clockType() default ClockType.GAME;

    /** 允许运行任务的服务类型 */
    ServiceType[] services() default {ServiceType.ALL};

    /** 最大连续失败次数。0表示失败后始终继续调度 */
    int maxConsecutiveFailures() default 0;
}
