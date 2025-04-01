/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.Week;
import java.util.Calendar;
import java.util.Date;

/** 周相关的工具类（注意，周相关的API使用此工具，以周日为每周第一天） @Date: 2024/11/20 下午8:23 @Author: xu.hai */
public class WeekUtil {
  /** 每周的开始时间（周日） */
  static final Week FIREST_WEEK = Week.SUNDAY;

  /** 指定时间是否是本周 */
  public static boolean isSameWeek(long millis) {
    return isSameWeek(System.currentTimeMillis(), millis);
  }

  /** 两个时间是否为同一周 */
  public static boolean isSameWeek(long millis1, long millis2) {
    return weekOfYear(millis1) == weekOfYear(millis2);
  }

  /** 获取年中周（自定义周的开始时间） */
  public static int weekOfYear(long millis) {
    return weekOfYear(new Date(millis));
  }

  /** 获取年中周（自定义周的开始时间） */
  public static int weekOfYear(Date date) {
    return dateNew(date).weekOfYear();
  }

  @SuppressWarnings("uncheck")
  public static Calendar calendar(Date date) {
    Calendar calendar = DateUtil.calendar(date);
    calendar.setFirstDayOfWeek(FIREST_WEEK.getValue());
    return calendar;
  }

  /** 获取日期对象（设置过周起始时间的） */
  public static DateTime dateNew() {
    return dateNew(new Date());
  }

  /** 获取日期对象（设置过周起始时间的） */
  public static DateTime dateNew(Date date) {
    return DateTime.of(date).setFirstDayOfWeek(FIREST_WEEK);
  }

  /** 获取偏移后的时间（offset：偏移量，如 0-本期 1-下一期 -1-上一期） */
  public static DateTime offset(int offset) {
    return offset(dateNew(), offset);
  }

  /** 获取偏移后的时间（offset：偏移量，如 0-本期 1-下一期 -1-上一期） */
  public static DateTime offset(Date date, int offset) {
    return dateNew(DateUtil.offset(dateNew(date), DateField.WEEK_OF_YEAR, offset));
  }

  /** 获取周的开始时间 */
  public static DateTime beginOfWeek() {
    return beginOfWeek(System.currentTimeMillis());
  }

  /** 获取周的开始时间 */
  public static DateTime beginOfWeek(long millis) {
    return beginOfWeek(new DateTime(millis));
  }

  /** 获取周的开始时间 */
  public static DateTime beginOfWeek(Date date) {
    return new DateTime(DateUtil.truncate(calendar(date), DateField.WEEK_OF_MONTH));
  }

  /** 获取周的结束时间 */
  public static DateTime endOfWeek() {
    return endOfWeek(System.currentTimeMillis());
  }

  /** 获取周的结束时间 */
  public static DateTime endOfWeek(long millis) {
    return endOfWeek(new DateTime(millis));
  }

  /** 获取周的结束时间 */
  public static DateTime endOfWeek(Date date) {
    return new DateTime(DateUtil.ceiling(calendar(date), DateField.WEEK_OF_MONTH));
  }

  /** 获取上周的开始时间 */
  public static DateTime lastWeekBegin() {
    return beginOfWeek(DateUtil.lastWeek());
  }

  /** 获取上周的结束时间 */
  public static DateTime lastWeekEnd() {
    return endOfWeek(DateUtil.lastWeek());
  }

  /** 获取下周的开始时间 */
  public static DateTime nextWeekBegin() {
    return beginOfWeek(DateUtil.nextWeek());
  }

  /** 获取下周的结束时间 */
  public static DateTime nextWeekEnd() {
    return endOfWeek(DateUtil.nextWeek());
  }

  public static void main(String[] args) {
    System.out.println("lastWeekBegin = " + lastWeekBegin());
    System.out.println("lastWeekEnd = " + lastWeekEnd());
    System.out.println("nextWeekBegin = " + nextWeekBegin());
    System.out.println("nextWeekEnd = " + nextWeekEnd());
  }
}
