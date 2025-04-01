/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import java.util.Calendar;
import java.util.Date;

/** 日期工具类 @Date: 2024/10/11 下午4:33 @Author: xu.hai */
public class DateUtil extends cn.hutool.core.date.DateUtil {

  public static int dayOfYear() {
    return dayOfYear(new Date());
  }

  /** 覆盖父类的 offsetWeek, 改为 WeekUtil中的offset，因为周日为起始时间 */
  public static DateTime offsetWeek(Date date, int offset) {
    return WeekUtil.offset(date, offset);
  }

  /** 获取今日零点 */
  public static DateTime beginOfDay() {
    return beginOfDay(new Date());
  }

  /** 指定时间是否是当天 */
  public static boolean isSameDay(long millis) {
    return isSameDay(System.currentTimeMillis(), millis);
  }

  /** 两个时间是否为同一天 */
  public static boolean isSameDay(long millis1, long millis2) {
    return isSameDay(new Date(millis1), new Date(millis2));
  }

  /** 指定时间是否是当月 */
  public static boolean isSameMonth(long millis) {
    return isSameMonth(System.currentTimeMillis(), millis);
  }

  /** 两个时间是否为同一月 */
  public static boolean isSameMonth(long millis1, long millis2) {
    return isSameMonth(new Date(millis1), new Date(millis2));
  }

  /** 判断指定时间是否为昨天 */
  public static boolean isYesterday(long millis) {
    return isSameDay(offsetDay(new DateTime(), -1), new DateTime(millis));
  }

  /** 格式化时间为 yyyy-MM-dd HH:mm:ss */
  public static String formatDateTime(long millis) {
    return formatDateTime(new Date(millis));
  }

  /** 格式化时间为 yyyyMMddHHmmss */
  public static String formatDateTimeKey(long millis) {
    return format(new Date(millis), DatePattern.PURE_DATETIME_FORMAT);
  }

  /** 格式化时间为 yyyyMMddHHmmss */
  public static long formatDateTimeKeyLong(long millis) {
    return Long.parseLong(formatDateTimeKey(millis));
  }

  /**
   * 获取两个日期相差几天
   * @return int
   * @date 2025/3/4 17:15
   */
  public static int getDaySpace(Date start, Date end) {
    if (start.after(end)) {
      Date t = start;
      start = end;
      end = t;
    }

    Calendar fromCalendar = parseCalendar(start);
    Calendar toCalendar = parseCalendar(end);

    return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 * 24));
  }

  private static Calendar parseCalendar(Date start) {
    Calendar fromCalendar = Calendar.getInstance();
    fromCalendar.setTime(start);
    fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
    fromCalendar.set(Calendar.MINUTE, 0);
    fromCalendar.set(Calendar.SECOND, 0);
    fromCalendar.set(Calendar.MILLISECOND, 0);
    return fromCalendar;
  }

  /**
   * 获取两个日期相差几个月
   * @param start start
   * @param end end
   * @return int
   * @date 2025/3/4 17:17
   */
  public static int getMonthSpace(Date start, Date end) {
    if (start.after(end)) {
      Date t = start;
      start = end;
      end = t;
    }

    Calendar startCalendar = Calendar.getInstance();
    startCalendar.setTime(start);
    Calendar endCalendar = Calendar.getInstance();
    endCalendar.setTime(end);
    Calendar temp = Calendar.getInstance();
    temp.setTime(end);
    temp.add(Calendar.DATE, 1);
    int year = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
    int month = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
    if ((startCalendar.get(Calendar.DATE) == 1) && (temp.get(Calendar.DATE) == 1)) {
      return year * 12 + month + 1;
    } else if ((startCalendar.get(Calendar.DATE) != 1) && (temp.get(Calendar.DATE) == 1)) {
      return year * 12 + month;
    } else if ((startCalendar.get(Calendar.DATE) == 1) && (temp.get(Calendar.DATE) != 1)) {
      return year * 12 + month;
    } else {
      return (year * 12 + month - 1) < 0 ? 0 : (year * 12 + month);
    }
  }

  /**
   * 获取两个日期相差几个年
   * @param start start
   * @param end end
   * @return int
   * @date 2025/3/4 17:17
   */
  public static int getYearSpace(Date start, Date end) {
    if (start.after(end)) {
      Date t = start;
      start = end;
      end = t;
    }

    Calendar fromCalendar = Calendar.getInstance();
    fromCalendar.setTime(start);

    Calendar toCalendar = Calendar.getInstance();
    toCalendar.setTime(end);

    return toCalendar.get(Calendar.YEAR) - fromCalendar.get(Calendar.YEAR);
  }
}
