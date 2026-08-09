/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.consts;

/**
 * ClockSource
 * 当前有效时间的来源
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public enum ClockSource {
    /** 系统时间 */
    SYSTEM,
    /** 游戏时间覆盖 */
    GAME,
    /** 玩家时间覆盖 */
    PLAYER
}
