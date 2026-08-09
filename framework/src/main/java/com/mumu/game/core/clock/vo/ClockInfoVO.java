/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.vo;

import com.mumu.game.core.clock.consts.ClockSource;

/**
 * ClockInfoVO
 * 当前进程中的有效时间快照
 * @param playerId 玩家ID，游戏时间快照时为null
 * @param source 有效时间来源
 * @param systemTime 系统时间戳
 * @param effectiveTime 有效时间戳
 * @param offsetMillis 相对系统时间的偏移毫秒数
 * @param version 修改版本
 * @param modifiedAt 修改时的系统时间戳
 * @param reason 修改原因
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public record ClockInfoVO(
    Long playerId,
    ClockSource source,
    long systemTime,
    long effectiveTime,
    long offsetMillis,
    long version,
    long modifiedAt,
    String reason) {
}
