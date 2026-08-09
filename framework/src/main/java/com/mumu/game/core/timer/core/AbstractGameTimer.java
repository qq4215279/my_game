/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

/**
 * AbstractGameTimer
 * 类级游戏周期性任务抽象基类
 * @author liuzhen
 * @version 1.0.0 2026/8/9 15:24
 */
public abstract class AbstractGameTimer implements Runnable {

    /**
     * 执行周期性任务
     */
    protected abstract void execute();

    /**
     * 统一任务执行入口
     */
    @Override
    public final void run() {
        execute();
    }
}
