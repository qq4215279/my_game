/*
 * Copyright 2020-2025, mumu without 996. All Right Reserved.
 */

package com.mumu.framework.core.util2;

/**
 * TimeUtil 时间工具类
 * 
 * @author liuzhen
 * @version 1.0.0 2024/8/15 17:11
 */
public class TimeUtil {
    public static final long ONE_SECOND_MILLIS = 1000;
    public static final long ONE_MINUTE_MILLIS = 60 * ONE_SECOND_MILLIS;
    public static final long ONE_HOUR_MILLIS = 60 * ONE_MINUTE_MILLIS;
    public static final long ONE_DAY_MILLIS = 24 * ONE_HOUR_MILLIS;
    public static final long ONE_WEEK_MILLIS = 7 * ONE_DAY_MILLIS;
    public static final long ONE_MONTH_MILLIS = 30 * ONE_DAY_MILLIS;
    public static final long ONE_DAY_SECONDS = 24 * 60 * 60;

    /** 获取当前时间（秒） */
    public static long nowSecond() {
        return System.currentTimeMillis() / ONE_SECOND_MILLIS;
    }
}
