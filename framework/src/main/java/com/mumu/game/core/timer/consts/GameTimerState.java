/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.consts;

/**
 * GameTimerState
 * 游戏周期性任务运行状态
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public enum GameTimerState {
    /** 已注册并等待执行 */
    SCHEDULED,
    /** 正在执行 */
    RUNNING,
    /** 已暂停，可以恢复或手动执行 */
    PAUSED,
    /** 已停止，不允许再次执行 */
    STOPPED
}
