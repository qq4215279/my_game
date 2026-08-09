/*
 * Copyright 2020-2026, mumu without 996. All Right Reserved.
 */

package com.mumu.game.core.clock.util;

import java.util.Date;

import com.mumu.game.core.clock.GameClock;
import com.mumu.game.core.utils.SpringContextUtils;

/**
 * TimeUtil
 * 时间工具类
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public class TimeUtil {
    /** 1秒毫秒值 */
    public static final long ONE_SECOND_MILLIS = 1000;
    /** 1分钟毫秒值 */
    public static final long ONE_MINUTE_MILLIS = 60 * ONE_SECOND_MILLIS;
    /** 1小时毫秒值 */
    public static final long ONE_HOUR_MILLIS = 60 * ONE_MINUTE_MILLIS;
    /** 1天毫秒值 */
    public static final long ONE_DAY_MILLIS = 24 * ONE_HOUR_MILLIS;
    /** 1周毫秒值 */
    public static final long ONE_WEEK_MILLIS = 7 * ONE_DAY_MILLIS;
    /** 1月毫秒值 */
    public static final long ONE_MONTH_MILLIS = 30 * ONE_DAY_MILLIS;
    /** 1天秒值 */
    public static final long ONE_DAY_SECONDS = 24 * 60 * 60;


    /**
     * 获取当前游戏时间
     * @return 当前游戏时间戳（毫秒）
     */
    public static long now() {
        return gameClock().gameTimeMillis();
    }

    /**
     * 获取当前游戏时间
     * @return 当前游戏时间
     */
    public static Date nowDate() {
        return new Date(now());
    }

    /**
     * 获取当前游戏时间
     * @return 当前游戏时间戳（秒）
     */
    public static long nowSecond() {
        return now() / ONE_SECOND_MILLIS;
    }

    /**
     * 获取当前系统时间（真实时间）
     * @return 当前系统时间戳（毫秒）
     */
    public static long systemNow() {
        return gameClock().systemTimeMillis();
    }

    /**
     * 获取当前系统时间（真实时间）
     * @return 当前系统时间
     */
    public static Date systemNowDate() {
        return new Date(systemNow());
    }

    /**
     * 获取当前系统时间（真实时间）
     * @return 当前系统时间戳（秒）
     */
    public static long systemNowSecond() {
        return systemNow() / ONE_SECOND_MILLIS;
    }

    /**
     * 获取当前玩家时间
     * @param playerId 玩家ID
     * @return 当前玩家时间戳（毫秒）
     */
    public static long playerNow(long playerId) {
        return gameClock().playerTimeMillis(playerId);
    }

    /**
     * 获取当前玩家时间
     * @param playerId 玩家ID
     * @return 当前玩家时间
     */
    public static Date playerNowDate(long playerId) {
        return new Date(playerNow(playerId));
    }

    /**
     * 获取当前玩家时间
     * @param playerId 玩家ID
     * @return 当前玩家时间戳（秒）
     */
    public static long playerNowSecond(long playerId) {
        return playerNow(playerId) / ONE_SECOND_MILLIS;
    }

    /**
     * 获取游戏时间服务
     * @return 游戏时间服务
     */
    private static GameClock gameClock() {
        return SpringContextUtils.getBean(GameClock.class);
    }
}
