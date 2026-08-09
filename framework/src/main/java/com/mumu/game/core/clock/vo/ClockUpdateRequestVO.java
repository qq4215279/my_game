/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.vo;

/**
 * ClockUpdateRequestVO
 * 时间修改请求
 * @param targetTimeMillis 目标时间戳
 * @param reason 修改原因
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public record ClockUpdateRequestVO(long targetTimeMillis, String reason) {
}
