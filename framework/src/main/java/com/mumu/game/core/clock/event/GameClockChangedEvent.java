/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.event;

import org.springframework.context.ApplicationEvent;

/**
 * GameClockChangedEvent
 * 当前进程游戏时间变化事件
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public class GameClockChangedEvent extends ApplicationEvent {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 修改前游戏时间戳 */
    private final long oldTime;
    /** 修改后游戏时间戳 */
    private final long newTime;
    /** 修改版本 */
    private final long version;
    /** 修改原因 */
    private final String reason;

    /**
     * 创建游戏时间变化事件
     * @param oldTime 修改前游戏时间戳
     * @param newTime 修改后游戏时间戳
     * @param version 修改版本
     * @param reason 修改原因
     */
    public GameClockChangedEvent(long oldTime, long newTime, long version, String reason) {
        super(GameClockChangedEvent.class);
        this.oldTime = oldTime;
        this.newTime = newTime;
        this.version = version;
        this.reason = reason;
    }

    /**
     * 判断游戏时间是否向前调整
     * @return true表示向前调整
     */
    public boolean forward() {
        return newTime > oldTime;
    }

    /**
     * 获取修改前游戏时间
     * @return 修改前游戏时间戳
     */
    public long oldTime() {
        return oldTime;
    }

    /**
     * 获取修改后游戏时间
     * @return 修改后游戏时间戳
     */
    public long newTime() {
        return newTime;
    }

    /**
     * 获取修改版本
     * @return 修改版本
     */
    public long version() {
        return version;
    }

    /**
     * 获取修改原因
     * @return 修改原因
     */
    public String reason() {
        return reason;
    }
}
