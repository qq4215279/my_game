/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.bo;

/**
 * ClockOverrideBO
 * JVM内时间覆盖信息
 * @param offsetMillis 相对系统时间的偏移毫秒数
 * @param version 修改版本
 * @param modifiedAt 修改时的系统时间戳
 * @param reason 修改原因
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public record ClockOverrideBO(long offsetMillis, long version, long modifiedAt, String reason) {

    /**
     * 根据系统时间计算有效时间
     * @param systemTimeMillis 系统时间戳
     * @return 有效时间戳
     */
    public long effectiveTime(long systemTimeMillis) {
        return Math.addExact(systemTimeMillis, offsetMillis);
    }
}
