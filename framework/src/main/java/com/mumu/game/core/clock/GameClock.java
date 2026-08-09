/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock;

import java.time.Instant;

import com.mumu.game.core.clock.vo.ClockInfoVO;

/**
 * GameClock
 * 系统、游戏和玩家三级时间统一入口
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public interface GameClock {

    /**
     * 获取系统时间（真实时间）
     * @return 系统时间戳
     */
    long systemTimeMillis();

    /**
     * 获取游戏时间
     * @return 游戏时间戳
     */
    long gameTimeMillis();

    /**
     * 获取玩家时间，优先级为玩家时间、游戏时间、系统时间
     * @param playerId 玩家ID
     * @return 玩家有效时间戳
     */
    long playerTimeMillis(long playerId);

    /**
     * 获取系统时间
     * @return 系统时间
     */
    default Instant systemInstant() {
        return Instant.ofEpochMilli(systemTimeMillis());
    }

    /**
     * 获取游戏时间
     * @return 游戏时间
     */
    default Instant gameInstant() {
        return Instant.ofEpochMilli(gameTimeMillis());
    }

    /**
     * 获取玩家时间
     * @param playerId 玩家ID
     * @return 玩家时间
     */
    default Instant playerInstant(long playerId) {
        return Instant.ofEpochMilli(playerTimeMillis(playerId));
    }

    /**
     * 获取游戏时间快照
     * @return 游戏时间快照
     */
    ClockInfoVO gameSnapshot();

    /**
     * 获取玩家时间快照
     * @param playerId 玩家ID
     * @return 玩家时间快照
     */
    ClockInfoVO playerSnapshot(long playerId);

    /**
     * 设置游戏时间
     * @param targetTimeMillis 目标时间戳
     * @param reason 修改原因
     * @return 修改后的时间快照
     */
    ClockInfoVO setGameTime(long targetTimeMillis, String reason);

    /**
     * 重置游戏时间
     * @param reason 修改原因
     * @return 重置后的时间快照
     */
    ClockInfoVO resetGameTime(String reason);

    /**
     * 设置玩家时间
     * @param playerId 玩家ID
     * @param targetTimeMillis 目标时间戳
     * @param reason 修改原因
     * @return 修改后的时间快照
     */
    ClockInfoVO setPlayerTime(long playerId, long targetTimeMillis, String reason);

    /**
     * 重置玩家时间
     * @param playerId 玩家ID
     * @param reason 修改原因
     * @return 重置后的时间快照
     */
    ClockInfoVO resetPlayerTime(long playerId, String reason);

    /**
     * 清理全部玩家时间覆盖
     * @param reason 修改原因
     * @return 实际清理数量
     */
    int resetAllPlayerTime(String reason);

}
