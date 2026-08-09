/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.vo;

import java.util.concurrent.TimeUnit;

/**
 * FixedDelayUpdateRequest
 * 更新任务固定延迟请求
 * @param delay 固定延迟
 * @param timeUnit 时间单位
 * @author liuzhen
 * @version 1.0.0 2026/8/9 14:38
 */
public record FixedDelayUpdateRequestVO(long delay, TimeUnit timeUnit) {
}
