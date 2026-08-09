/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.consts;

/**
 * ClockType
 * 定时任务使用的时间类型
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public enum ClockType {
    /** 真实系统时间，不受GM时间调整影响 */
    SYSTEM,
    /** 游戏时间，未设置偏移时自动回退到系统时间 */
    GAME
}
