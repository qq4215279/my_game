/*
 * Copyright 2020-2023, 木木996.
 * All Right Reserved.
 */

package com.mumu.common.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * DateUtil
 * 日期工具类
 * @author liuzhen
 * @version 1.0.0 2022/4/30 17:15
 */
public class DateUtils {
    private static Log log = LogFactory.getLog(DateUtils.class);

    private static final char DATE_HYPHEN = '-';
    private static final char DATE_SLASH = '/';
    private static final char DATE_SPLITER = ' ';
    private static final int SPLIT_YMD_COUNT = 3;
    private static final String DATETIME_SEPARATOR_FULL = "yyyy MM dd HH mm ss";
    private static final String DATETIME_SEPARATOR_YMDHM = "yyyy MM dd HH mm";
    private static final String DATE_SEPARATOR_FULL = "yyyy MM dd";
    private static final String DATE_SEPARATOR_YM = "yyyy MM";
    public static final String DATETIME_FULLSLASH = "yyyy/MM/dd HH:mm:ss";
    public static final String DATETIME_HM_SLASH = "yyyy/MM/dd HH:mm";
    public static final String DATE_FULLSLASH = "yyyy/MM/dd";
    public static final String DATE_YM_SLASH = "yyyy/MM";
    public static final String DATETIME_FULLHYPHEN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_HM_HYPHEN = "yyyy-MM-dd HH:mm";
    public static final String DATE_FULLHYPHEN = "yyyy-MM-dd";
    public static final String DATE_YM_HYPHEN = "yyyy-MM";
    public static final String DATE_HM = "HH:mm";
    public static final String DATE_MS = "mm:SS";
    public static final String DATETIME_COMPACT_FULL_S = "yyyyMMddHHmmssS";
    public static final String DATETIME_COMPACT_FULL = "yyyyMMddHHmmss";
    public static final String DATETIME_COMPACT_YMDHM = "yyyyMMddHHmm";
    public static final String DATE_COMPACT_FULL = "yyyyMMdd";
    public static final String DATE_COMPACT_YM = "yyyyMM";
    public static final String DATE_YY = "yy";
    private static final String V_DATETIME_FULLSLASH = "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2} [0-9]{2}\\:[0-9]{2}\\:[0-9]{2}";
    private static final String V_DATETIME_HM_SLASH = "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2} [0-9]{2}\\:[0-9]{2}";
    private static final String V_DATE_FULLSLASH = "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}";
    private static final String V_DATE_YM_SLASH = "[0-9]{4}\\/[0-9]{2}";
    private static final long MINUTE_SEC = 60000L;
    private static final Map<String, String> SEPARATE_MAP = Collections.synchronizedMap(new HashMap());
    private static final Map<Integer, String> CONVERT_MAP = Collections.synchronizedMap(new HashMap());
    private static final Map<Integer, String> VALIDATE_MAP = Collections.synchronizedMap(new HashMap());

    static {
        SEPARATE_MAP.put("yyyy/MM/dd HH:mm:ss", "yyyy MM dd HH mm ss");
        SEPARATE_MAP.put("yyyy/MM/dd HH:mm", "yyyy MM dd HH mm");
        SEPARATE_MAP.put("yyyy/MM/dd", "yyyy MM dd");
        SEPARATE_MAP.put("yyyy/MM", "yyyy MM");
        SEPARATE_MAP.put("yyyy-MM-dd HH:mm:ss", "yyyy MM dd HH mm ss");
        SEPARATE_MAP.put("yyyy-MM-dd HH:mm", "yyyy MM dd HH mm");
        SEPARATE_MAP.put("yyyy-MM-dd", "yyyy MM dd");
        SEPARATE_MAP.put("yyyy-MM", "yyyy MM");
        SEPARATE_MAP.put("yyyyMMddHHmmss", "yyyy MM dd HH mm ss");
        SEPARATE_MAP.put("yyyyMMddHHmm", "yyyy MM dd HH mm");
        SEPARATE_MAP.put("yyyyMMdd", "yyyy MM dd");
        SEPARATE_MAP.put("yyyyMM", "yyyy MM");
        CONVERT_MAP.put("yyyy/MM/dd HH:mm:ss".length(), "yyyy/MM/dd HH:mm:ss");
        CONVERT_MAP.put("yyyy/MM/dd HH:mm".length(), "yyyy/MM/dd HH:mm");
        CONVERT_MAP.put("yyyy/MM/dd".length(), "yyyy/MM/dd");
        CONVERT_MAP.put("yyyy/MM".length(), "yyyy/MM");
        CONVERT_MAP.put("yyyyMMddHHmmss".length(), "yyyyMMddHHmmss");
        CONVERT_MAP.put("yyyyMMddHHmm".length(), "yyyyMMddHHmm");
        CONVERT_MAP.put("yyyyMMdd".length(), "yyyyMMdd");
        CONVERT_MAP.put("yyyyMM".length(), "yyyyMM");
        VALIDATE_MAP.put("yyyy/MM/dd HH:mm:ss".length(), "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2} [0-9]{2}\\:[0-9]{2}\\:[0-9]{2}");
        VALIDATE_MAP.put("yyyy/MM/dd HH:mm".length(), "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2} [0-9]{2}\\:[0-9]{2}");
        VALIDATE_MAP.put("yyyy/MM/dd".length(), "[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}");
        VALIDATE_MAP.put("yyyy/MM".length(), "[0-9]{4}\\/[0-9]{2}");
        VALIDATE_MAP.put("yyyyMMddHHmmss".length(), "[0-9]{14}");
        VALIDATE_MAP.put("yyyyMMddHHmm".length(), "[0-9]{12}");
        VALIDATE_MAP.put("yyyyMMdd".length(), "[0-9]{8}");
        VALIDATE_MAP.put("yyyyMM".length(), "[0-9]{6}");
    }

    public DateUtils() {
    }

    public static String formatDatetime(String strSrc) {
        if (strSrc != null) {
            if (strSrc.length() != 10) {
                return null;
            } else {
                String sDate = strSrc.replace(String.valueOf('-'), String.valueOf('/'));
                sDate = sDate.substring(0, 10);
                return sDate;
            }
        } else {
            return null;
        }
    }

    public static String[] splitDateTime(String inDateStr) {
        String[] dateSplitArray = null;
        if (inDateStr != null) {
            if (inDateStr.indexOf(45) > 0) {
                dateSplitArray = inDateStr.split(String.valueOf('-'));
            } else {
                dateSplitArray = inDateStr.split(String.valueOf('/'));
            }

            if (dateSplitArray.length != 3) {
                dateSplitArray = null;
            }
        }

        return dateSplitArray;
    }

    /**
     * 日期转成指定类型字符串
     * @author liuzhen
     * @date 2022/6/6 17:05
     * @param date
     * @param pattern
     * @return java.lang.String
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return null;
        } else if (StringUtils.isBlank(pattern)) {
            return null;
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern);
            String convStr = fmt.format(date);
            return convStr;
        }
    }

    public static String[] split(Date date, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return null;
        } else {
            String convertPattern = (String)SEPARATE_MAP.get(pattern);
            String[] outArray = null;
            if (date == null) {
                return outArray;
            } else {
                if (convertPattern != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat(convertPattern);
                    String outStr = fmt.format(date);
                    outArray = outStr.split(String.valueOf(' '));
                }

                return outArray;
            }
        }
    }

    /**
     * 字符串转换成指定格式日期
     * @author liuzhen
     * @date 2022/6/6 17:06
     * @param inDate
     * @return java.util.Date
     */
    public static Date parseDate(String inDate) {
        if (StringUtils.isBlank(inDate)) {
            return null;
        } else if (!validate(inDate)) {
            return null;
        } else {
            int length = inDate.length();
            String convPattern = (String)CONVERT_MAP.get(length);
            if (convPattern == null) {
                return null;
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat(convPattern);
                formatter.setLenient(false);
                Date date = null;

                try {
                    date = formatter.parse(inDate);
                } catch (ParseException var6) {
                    log.warn("date parse error", var6);
                }

                return date;
            }
        }
    }

    private static boolean validate(String inData) {
        if (StringUtils.isEmpty(inData)) {
            return false;
        } else {
            String checkPattern = (String)VALIDATE_MAP.get(inData.length());
            if (checkPattern == null) {
                return false;
            } else {
                boolean isValidate = inData.matches(checkPattern);
                return isValidate;
            }
        }
    }

    public static String convertFullYMDHMS(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyy/MM/dd HH:mm:ss");
    }

    public static String convertFullYMD(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyy/MM/dd");
    }

    public static String convertFullYM(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyy/MM");
    }

    public static String convertCompactYMDHMS(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyyMMddHHmmss");
    }

    public static String convertCompactYMD(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyyMMdd");
    }

    public static String convertCompactYM(String inDateString) {
        Date inDate = parseDate(inDateString);
        return formatDate(inDate, "yyyyMM");
    }

    /**
     * 日期转成日历类
     * @author liuzhen
     * @date 2022/6/6 17:07
     * @param date
     * @return java.util.Calendar
     */
    public static Calendar convertCalendar(Date date) {
        Calendar calendar = null;
        if (date != null) {
            calendar = Calendar.getInstance();
            calendar.setTime(date);
        }

        return calendar;
    }

    /**
     * 日历转日期
     * @author liuzhen
     * @date 2022/6/6 17:09
     * @param cal
     * @return java.util.Date
     */
    public static Date convertDate(Calendar cal) {
        Date date = null;
        if (cal != null) {
            date = cal.getTime();
        }

        return date;
    }

    /**
     * 是否在同一月份
     * @author liuzhen
     * @date 2022/6/6 17:18
     * @param date1
     * @param date2
     * @return boolean
     */
    public static boolean isSameMonth(Date date1, Date date2) {
        Calendar cg1 = Calendar.getInstance();
        Calendar cg2 = Calendar.getInstance();
        cg1.setTime(date1);
        cg2.setTime(date2);
        return cg1.get(Calendar.YEAR) == cg2.get(Calendar.YEAR) && cg1.get(Calendar.MONTH) == cg2.get(Calendar.MONTH);
    }

    /**
     * 是否在相同周
     * @author liuzhen
     * @date 2022/6/6 17:17
     * @param date1 日期1
     * @param date2 日期2
     * @return boolean
     */
    public static boolean isSameWeek(Date date1, Date date2) {
        Calendar cg1 = Calendar.getInstance();
        Calendar cg2 = Calendar.getInstance();
        if (date1.after(date2)) {
            cg1.setTime(date2);
            cg2.setTime(date1);
        } else {
            cg1.setTime(date1);
            cg2.setTime(date2);
        }

        int yearWeek1 = cg1.get(Calendar.WEEK_OF_YEAR);
        int yearWeek2 = cg2.get(Calendar.WEEK_OF_YEAR);
        int weekDay1 = cg1.get(Calendar.DAY_OF_WEEK);
        int weekDay2 = cg2.get(Calendar.DAY_OF_WEEK);
        if (weekDay1 != 1) {
            ++yearWeek1;
        }

        if (weekDay2 != 1) {
            ++yearWeek2;
        }

        return cg1.get(Calendar.YEAR) == cg2.get(Calendar.YEAR) && yearWeek1 == yearWeek2;
    }

    /**
     * 是否是相同一天
     * @author liuzhen
     * @date 2022/6/6 17:18
     * @param date1
     * @param date2
     * @return boolean
     */
    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cg1 = Calendar.getInstance();
        Calendar cg2 = Calendar.getInstance();
        cg1.setTime(date1);
        cg2.setTime(date2);
        return cg1.get(Calendar.YEAR) == cg2.get(Calendar.YEAR) && cg1.get(Calendar.DAY_OF_YEAR) == cg2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 是否是相同一天
     * @author liuzhen
     * @date 2022/6/6 17:19
     * @param timestamp1
     * @param timestamp2
     * @return boolean
     */
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cg1 = Calendar.getInstance();
        Calendar cg2 = Calendar.getInstance();
        cg1.setTimeInMillis(timestamp1);
        cg2.setTimeInMillis(timestamp2);
        return cg1.get(Calendar.YEAR) == cg2.get(Calendar.YEAR) && cg1.get(Calendar.DAY_OF_YEAR) == cg2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 毫米值转格式化字符串
     * @author liuzhen
     * @date 2022/6/6 17:19
     * @param time 毫秒值
     * @return java.lang.String
     */
    public static String formatTime(long time) {
        long minute = time / 60000L;
        time %= 60000L;
        long sec = time / 1000L;
        return minute + ":" + sec;
    }

    /**
     * 获取本月的第一天
     * @author liuzhen
     * @date 2022/6/6 17:20
     * @param date
     * @return java.util.Date
     */
    public static Date getMonthStart(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.set(Calendar.DAY_OF_MONTH, 1);
        return cg.getTime();
    }

    /**
     * 获取本月的最后一天
     * @author liuzhen
     * @date 2022/6/6 17:20
     * @param date
     * @return java.util.Date
     */
    public static Date getMonthEnd(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.set(Calendar.DAY_OF_MONTH, 1);
        cg.set(Calendar.MONTH, cg.get(Calendar.MONTH) + 1);
        cg.set(Calendar.DAY_OF_YEAR, -1);
        cg.set(Calendar.HOUR_OF_DAY, 23);
        cg.set(Calendar.MINUTE, 59);
        cg.set(Calendar.SECOND, 59);
        cg.set(Calendar.MILLISECOND, 999);
        return cg.getTime();
    }

    /**
     * 获取本周的开始日期
     * @author liuzhen
     * @date 2022/6/6 17:43
     * @param date
     * @return java.util.Date
     */
    public static Date getWeekStart(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.add(Calendar.DATE, -1);
        cg.set(Calendar.DAY_OF_WEEK, 2);
        cg.set(Calendar.HOUR_OF_DAY, 0);
        cg.set(Calendar.MINUTE, 0);
        cg.set(Calendar.SECOND, 0);
        cg.set(Calendar.MILLISECOND, 0);
        return cg.getTime();
    }

    public static void main(String[] args) {
        System.out.println(getWeekStart(new Date()));
    }

    /**
     * 获取本周结束日期
     * @author liuzhen
     * @date 2022/6/6 17:44
     * @param date
     * @return java.util.Date
     */
    public static Date getWeekEnd(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.add(Calendar.DATE, 6);
        cg.set(Calendar.DAY_OF_WEEK, 2);
        cg.add(Calendar.DAY_OF_YEAR, -1);

        cg.set(Calendar.HOUR_OF_DAY, 23);
        cg.set(Calendar.MINUTE, 59);
        cg.set(Calendar.SECOND, 59);
        cg.set(Calendar.MILLISECOND, 999);
        return cg.getTime();
    }

    /**
     * 获取当天开始时间
     * @author liuzhen
     * @date 2022/6/6 17:44
     * @param date
     * @return java.util.Date
     */
    public static Date getDayStart(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.set(Calendar.HOUR_OF_DAY, 0);
        cg.set(Calendar.MINUTE, 0);
        cg.set(Calendar.SECOND, 0);
        cg.set(Calendar.MILLISECOND, 0);
        return cg.getTime();
    }

    /**
     * 获取当天结束时间
     * @author liuzhen
     * @date 2022/6/6 17:44
     * @param date
     * @return java.util.Date
     */
    public static Date getDayEnd(Date date) {
        Calendar cg = Calendar.getInstance();
        cg.setTime(date);
        cg.set(Calendar.HOUR_OF_DAY, 23);
        cg.set(Calendar.MINUTE, 59);
        cg.set(Calendar.SECOND, 59);
        cg.set(Calendar.MILLISECOND, 999);
        return cg.getTime();
    }

    public static boolean isInterval(Date startTime, Date endTime, int hour, int min) {
        Calendar cg = Calendar.getInstance();
        cg.set(Calendar.HOUR_OF_DAY, hour);
        cg.set(Calendar.MINUTE, min);
        cg.set(Calendar.SECOND, 0);
        cg.set(Calendar.MILLISECOND, 0);

        Date clearPoint = cg.getTime();
        Calendar cg1 = Calendar.getInstance();
        Calendar cg2 = Calendar.getInstance();
        Calendar cg3 = Calendar.getInstance();
        cg1.setTime(startTime);
        cg2.setTime(endTime);
        cg3.setTime(clearPoint);
        cg3.add(5, -1);
        return clearPoint.after(cg1.getTime()) && (endTime.after(clearPoint) || cg1.before(cg3));
    }

    public static int getIntervalHalfHourNum(Date startDate, Date endDate) {
        if (startDate != null && endDate != null && !endDate.before(startDate)) {
            Calendar now = Calendar.getInstance();
            now.setTime(endDate);
            int nowMinute = now.get(12);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Calendar last = Calendar.getInstance();
            last.setTime(startDate);
            int lastMinute = last.get(12);
            last.set(Calendar.MINUTE, 0);
            last.set(Calendar.SECOND, 0);
            last.set(Calendar.MILLISECOND, 0);
            int num = (int)((now.getTimeInMillis() - last.getTimeInMillis()) / 3600000L);
            num *= 2;
            if (nowMinute >= 30 && lastMinute < 30) {
                ++num;
            } else if (nowMinute < 30 && lastMinute >= 30) {
                --num;
                num = num > 0 ? num : 0;
            }

            return num;
        } else {
            return 0;
        }
    }

    public static int getIntervalHourNum(Date startDate, Date endDate) {
        if (startDate != null && endDate != null && !endDate.before(startDate)) {
            Calendar now = Calendar.getInstance();
            now.setTime(endDate);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Calendar last = Calendar.getInstance();
            last.setTime(startDate);
            last.set(Calendar.MINUTE, 0);
            last.set(Calendar.SECOND, 0);
            last.set(Calendar.MILLISECOND, 0);
            int num = (int)((now.getTimeInMillis() - last.getTimeInMillis()) / 3600000L);
            return num;
        } else {
            return 0;
        }
    }
}
