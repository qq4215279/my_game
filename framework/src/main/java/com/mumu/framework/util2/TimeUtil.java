package com.mumu.framework.util2;

/** 日期工具类 @Date: 2024/9/2 下午7:44 @Author: xu.hai */
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
