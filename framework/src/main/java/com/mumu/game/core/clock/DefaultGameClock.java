/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.mumu.game.business.system.luban.SystemSwitch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mumu.game.core.clock.bo.ClockOverrideBO;
import com.mumu.game.core.clock.vo.ClockInfoVO;
import com.mumu.game.core.clock.config.GameClockProperties;
import com.mumu.game.core.clock.consts.ClockSource;
import com.mumu.game.core.clock.event.GameClockChangedEvent;
import com.mumu.game.core.clock.event.PlayerClockChangedEvent;
import com.mumu.game.core.utils.SpringContextUtils;

/**
 * DefaultGameClock
 * 基于JVM内存偏移量实现的游戏时间服务
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
@Component
public class DefaultGameClock implements GameClock {

    /** 游戏时间配置 */
    private final GameClockProperties properties;
    /** 底层系统时钟 */
    private final Clock systemClock;
    /** 全局游戏时间覆盖 */
    private final AtomicReference<ClockOverrideBO> gameOverride = new AtomicReference<>();
    /** 玩家时间覆盖，当前版本仅在本JVM内生效 */
    private final Map<Long, ClockOverrideBO> playerOverrides = new ConcurrentHashMap<>();
    /** 时间修改版本序列 */
    private final AtomicLong versionSequence = new AtomicLong();

    /**
     * 创建游戏时间服务
     * @param properties 游戏时间配置
     */
    @Autowired
    public DefaultGameClock(GameClockProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * 创建可注入底层时钟的游戏时间服务
     * @param properties 游戏时间配置
     * @param systemClock 底层系统时钟
     */
    public DefaultGameClock(GameClockProperties properties, Clock systemClock) {
        if (properties == null || systemClock == null) {
            throw new IllegalArgumentException("游戏时间服务依赖不能为空");
        }
        this.properties = properties;
        this.systemClock = systemClock;
    }

    @Override
    public long systemTimeMillis() {
        return systemClock.millis();
    }

    @Override
    public long gameTimeMillis() {
        long systemTime = systemTimeMillis();
        return effectiveTime(systemTime, gameOverride.get());
    }

    @Override
    public long playerTimeMillis(long playerId) {
        validatePlayerId(playerId);
        long systemTime = systemTimeMillis();
        ClockOverrideBO playerOverride = playerOverrides.get(playerId);
        return playerOverride == null ? effectiveTime(systemTime, gameOverride.get())
            : playerOverride.effectiveTime(systemTime);
    }

    @Override
    public ClockInfoVO gameSnapshot() {
        long systemTime = systemTimeMillis();
        ClockOverrideBO override = gameOverride.get();
        return snapshot(null, override == null ? ClockSource.SYSTEM : ClockSource.GAME, systemTime, override);
    }

    @Override
    public ClockInfoVO playerSnapshot(long playerId) {
        validatePlayerId(playerId);
        long systemTime = systemTimeMillis();
        ClockOverrideBO playerOverride = playerOverrides.get(playerId);
        if (playerOverride != null) {
            return snapshot(playerId, ClockSource.PLAYER, systemTime, playerOverride);
        }
        ClockOverrideBO currentGameOverride = gameOverride.get();
        ClockSource source = currentGameOverride == null ? ClockSource.SYSTEM : ClockSource.GAME;
        return snapshot(playerId, source, systemTime, currentGameOverride);
    }

    @Override
    public ClockInfoVO setGameTime(long targetTimeMillis, String reason) {
        checkMutationAllowed();
        validateTargetTime(targetTimeMillis);
        long systemTime = systemTimeMillis();
        long oldTime = effectiveTime(systemTime, gameOverride.get());
        ClockOverrideBO override = newOverride(targetTimeMillis, systemTime, reason);
        gameOverride.set(override);
        SpringContextUtils.publishEvent(new GameClockChangedEvent(
            oldTime, targetTimeMillis, override.version(), override.reason()));
        return snapshot(null, ClockSource.GAME, systemTime, override);
    }

    @Override
    public ClockInfoVO resetGameTime(String reason) {
        checkMutationAllowed();
        long systemTime = systemTimeMillis();
        ClockOverrideBO oldOverride = gameOverride.getAndSet(null);
        if (oldOverride != null) {
            long version = versionSequence.incrementAndGet();
            SpringContextUtils.publishEvent(new GameClockChangedEvent(
                oldOverride.effectiveTime(systemTime), systemTime, version, normalizeReason(reason)));
        }
        return snapshot(null, ClockSource.SYSTEM, systemTime, null);
    }

    @Override
    public ClockInfoVO setPlayerTime(long playerId, long targetTimeMillis, String reason) {
        checkMutationAllowed();
        validatePlayerId(playerId);
        validateTargetTime(targetTimeMillis);
        long systemTime = systemTimeMillis();
        long oldTime = playerTimeMillisAt(playerId, systemTime);
        ClockOverrideBO override = newOverride(targetTimeMillis, systemTime, reason);
        playerOverrides.put(playerId, override);
        SpringContextUtils.publishEvent(new PlayerClockChangedEvent(
            playerId, oldTime, targetTimeMillis, override.version(), override.reason()));
        return snapshot(playerId, ClockSource.PLAYER, systemTime, override);
    }

    @Override
    public ClockInfoVO resetPlayerTime(long playerId, String reason) {
        checkMutationAllowed();
        validatePlayerId(playerId);
        long systemTime = systemTimeMillis();
        ClockOverrideBO oldOverride = playerOverrides.remove(playerId);
        ClockOverrideBO currentGameOverride = gameOverride.get();
        if (oldOverride != null) {
            long newTime = effectiveTime(systemTime, currentGameOverride);
            long version = versionSequence.incrementAndGet();
            SpringContextUtils.publishEvent(new PlayerClockChangedEvent(
                playerId, oldOverride.effectiveTime(systemTime), newTime, version, normalizeReason(reason)));
        }
        ClockSource source = currentGameOverride == null ? ClockSource.SYSTEM : ClockSource.GAME;
        return snapshot(playerId, source, systemTime, currentGameOverride);
    }

    @Override
    public int resetAllPlayerTime(String reason) {
        checkMutationAllowed();
        int resetCount = 0;
        for (Long playerId : playerOverrides.keySet()) {
            ClockOverrideBO oldOverride = playerOverrides.remove(playerId);
            if (oldOverride == null) {
                continue;
            }
            long systemTime = systemTimeMillis();
            long newTime = effectiveTime(systemTime, gameOverride.get());
            long version = versionSequence.incrementAndGet();
            SpringContextUtils.publishEvent(new PlayerClockChangedEvent(
                playerId, oldOverride.effectiveTime(systemTime), newTime, version, normalizeReason(reason)));
            resetCount++;
        }
        return resetCount;
    }

    /**
     * 创建新的时间覆盖
     * @param targetTimeMillis 目标时间戳
     * @param systemTime 系统时间戳
     * @param reason 修改原因
     * @return 时间覆盖
     */
    private ClockOverrideBO newOverride(long targetTimeMillis, long systemTime, String reason) {
        long offset = Math.subtractExact(targetTimeMillis, systemTime);
        return new ClockOverrideBO(offset, versionSequence.incrementAndGet(), systemTime, normalizeReason(reason));
    }

    /**
     * 创建时间快照
     * @param playerId 玩家ID
     * @param source 时间来源
     * @param systemTime 系统时间戳
     * @param override 时间覆盖
     * @return 时间快照
     */
    private ClockInfoVO snapshot(
        Long playerId,
        ClockSource source,
        long systemTime,
        ClockOverrideBO override) {
        if (override == null) {
            return new ClockInfoVO(playerId, source, systemTime, systemTime, 0L, 0L, 0L, "");
        }
        return new ClockInfoVO(playerId, source, systemTime, override.effectiveTime(systemTime),
            override.offsetMillis(), override.version(), override.modifiedAt(), override.reason());
    }

    /**
     * 计算指定玩家在同一系统时间点的有效时间
     * @param playerId 玩家ID
     * @param systemTime 系统时间戳
     * @return 玩家有效时间戳
     */
    private long playerTimeMillisAt(long playerId, long systemTime) {
        ClockOverrideBO playerOverride = playerOverrides.get(playerId);
        return playerOverride == null ? effectiveTime(systemTime, gameOverride.get())
            : playerOverride.effectiveTime(systemTime);
    }

    /**
     * 计算有效时间
     * @param systemTime 系统时间戳
     * @param override 时间覆盖
     * @return 有效时间戳
     */
    private long effectiveTime(long systemTime, ClockOverrideBO override) {
        return override == null ? systemTime : override.effectiveTime(systemTime);
    }

    /** 校验当前进程是否允许修改时间 */
    private void checkMutationAllowed() {
        if (SystemSwitch.GM.notGM()) {
            throw new IllegalStateException("游戏时间GM修改开关未开启");
        }
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
     * 校验目标时间
     * @param targetTimeMillis 目标时间戳
     */
    private void validateTargetTime(long targetTimeMillis) {
        if (targetTimeMillis <= 0L) {
            throw new IllegalArgumentException("目标时间戳必须大于0");
        }
    }

    /**
     * 标准化修改原因
     * @param reason 修改原因
     * @return 标准化后的修改原因
     */
    private String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim();
    }
}
