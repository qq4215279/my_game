/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock;

import com.mumu.game.core.clock.vo.ClockInfoVO;
import com.mumu.game.core.clock.consts.ClockSource;

/**
 * SystemGameClock
 * 仅使用系统时间的只读实现，主要用于脱离Spring容器的测试场景
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public final class SystemGameClock implements GameClock {

    /** 单例实例 */
    public static final SystemGameClock INSTANCE = new SystemGameClock();

    /** 禁止外部重复创建 */
    private SystemGameClock() {
    }

    @Override
    public long systemTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long gameTimeMillis() {
        return systemTimeMillis();
    }

    @Override
    public long playerTimeMillis(long playerId) {
        validatePlayerId(playerId);
        return systemTimeMillis();
    }

    @Override
    public ClockInfoVO gameSnapshot() {
        return systemSnapshot(null);
    }

    @Override
    public ClockInfoVO playerSnapshot(long playerId) {
        validatePlayerId(playerId);
        return systemSnapshot(playerId);
    }

    @Override
    public ClockInfoVO setGameTime(long targetTimeMillis, String reason) {
        throw unsupported();
    }

    @Override
    public ClockInfoVO resetGameTime(String reason) {
        throw unsupported();
    }

    @Override
    public ClockInfoVO setPlayerTime(long playerId, long targetTimeMillis, String reason) {
        throw unsupported();
    }

    @Override
    public ClockInfoVO resetPlayerTime(long playerId, String reason) {
        throw unsupported();
    }

    @Override
    public int resetAllPlayerTime(String reason) {
        throw unsupported();
    }

    /**
     * 创建系统时间快照
     * @param playerId 玩家ID
     * @return 系统时间快照
     */
    private ClockInfoVO systemSnapshot(Long playerId) {
        long systemTime = systemTimeMillis();
        return new ClockInfoVO(playerId, ClockSource.SYSTEM, systemTime, systemTime, 0L, 0L, 0L, "");
    }

    /**
     * 校验玩家ID
     * @param playerId 玩家ID
     */
    private void validatePlayerId(long playerId) {
        if (playerId <= 0L) {
            throw new IllegalArgumentException("玩家ID必须大于0");
        }
    }

    /**
     * 创建只读实现异常
     * @return 不支持修改异常
     */
    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("系统时间实现不支持修改时间");
    }
}
