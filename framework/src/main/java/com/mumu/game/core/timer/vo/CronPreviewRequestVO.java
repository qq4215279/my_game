/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.vo;

/**
 * CronPreviewRequest
 * Cron校验和执行时间预览请求
 * @param cron Spring六段式Cron表达式
 * @param count 预览次数
 * @param zone 时区，空字符串表示系统默认时区
 * @author liuzhen
 * @version 1.0.0 2026/8/9 14:38
 */
public record CronPreviewRequestVO(String cron, Integer count, String zone) {
}
