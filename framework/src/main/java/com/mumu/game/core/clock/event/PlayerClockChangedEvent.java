/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.event;

import org.springframework.context.ApplicationEvent;

/**
 * PlayerClockChangedEvent
 * 当前进程玩家时间变化事件
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public class PlayerClockChangedEvent extends ApplicationEvent {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 玩家ID */
    private final long playerId;
    /** 修改前玩家时间戳 */
    private final long oldTime;
    /** 修改后玩家时间戳 */
    private final long newTime;
    /** 修改版本 */
    private final long version;
    /** 修改原因 */
    private final String reason;

    /**
     * 创建玩家时间变化事件
     * @param playerId 玩家ID
     * @param oldTime 修改前玩家时间戳
     * @param newTime 修改后玩家时间戳
     * @param version 修改版本
     * @param reason 修改原因
     */
    public PlayerClockChangedEvent(
        long playerId,
        long oldTime,
        long newTime,
        long version,
        String reason) {
        super(PlayerClockChangedEvent.class);
        this.playerId = playerId;
        this.oldTime = oldTime;
        this.newTime = newTime;
        this.version = version;
        this.reason = reason;
    }

    /**
     * 获取玩家ID
     * @return 玩家ID
     */
    public long playerId() {
        return playerId;
    }

    /**
     * 获取修改前玩家时间
     * @return 修改前玩家时间戳
     */
    public long oldTime() {
        return oldTime;
    }

    /**
     * 获取修改后玩家时间
     * @return 修改后玩家时间戳
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
