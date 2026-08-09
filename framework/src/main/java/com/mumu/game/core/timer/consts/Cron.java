package com.mumu.game.core.timer.consts;

import com.mumu.game.core.timer.util.CronUtil;

import java.util.Date;

/**
 * Cron
 * 常见 cron表达式
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:55
 */
public interface Cron {
    /** 每秒 */
    String EVERY_SECOND = "*/1 * * * * ?";
    /** 每5秒 */
    String EVERY_5_SECOND = "*/5 * * * * ?";
    /** 每10秒 */
    String EVERY_10_SECOND = "*/10 * * * * ?";
    /** 每30秒 */
    String EVERY_30_SECOND = "*/30 * * * * ?";
    /** 每1分钟 */
    String EVERY_MINUTE = "0 */1 * * * ?";
    /** 每2分钟 */
    String EVENT_2_MINUTE = "0 */2 * * * ?";
    /** 每3分钟 */
    String EVENT_3_MINUTE = "0 */3 * * * ?";
    /** 每整5分钟 */
    String EVERY_5_MINUTE = "0 */5 * * * ?";
    /** 每整10分钟 */
    String EVERY_10_MINUTE = "0 */10 * * * ?";
    /** 每整15分钟 */
    String EVERY_15_MINUTE = "0 */15 * * * ?";
    /** 每整30分钟 */
    String EVERY_30_MINUTE = "0 */30 * * * ?";
    /** 每1小时（整点） */
    String EVERY_HOUR = "0 0 */1 * * ?";
    /** 每1小时（0分15秒） */
    String EVERY_HOUR_0_15 = "15 0 */1 * * ?";
    /** 每1小时（59分） */
    String EVERY_HOUR_59 = "0 59 */1 * * ?";
    /** 每2小时（整点） */
    String EVERY_2_HOUR = "0 0 */2 * * ?";
    /** 每6小时（整点） */
    String EVERY_6_HOUR = "0 0 */6 * * ?";
    /** 每日0点0分 */
    String EVERY_DAY_0_0 = "0 0 0 * * ?";
    /** 每日2点 */
    String EVERY_DAY_2 = "0 0 2 * * ?";
    /** 每日5点 */
    String EVERY_DAY_5 = "0 0 5 * * ?";
    /** 每日23点59分 */
    String EVERY_DAY_23_59 = "0 59 23 * * ?";
    /** 每周日23点40分 */
    String EVERY_SUN_23_40 = "0 40 23 ? * SUN";
    /** 每周一00:00:00 */
    String EVERY_MON_0_0 = "0 0 0 ? * MON";
    /** 每周一00:30:00 */
    String EVERY_MON_0_30 = "0 30 0 ? * Mon";
    /** 每周一00:00:10 */
    String EVERY_MON_0_0_10 = "10 0 0 ? * Mon";
    /** 每天指定时间段全量同步当日道具封控数据 */
    String EVENT_RISK_FULL = "0 0 4,8,12,16,20 * * ?";


    /* ==================================== 表达式校验 ================================ */
    public static void main(String[] args) {
        String cronExpression = "0 0 12 1 6 ?";
        // 打印结果
        System.out.printf("当前时间：%tF %<tT%n", new Date());
        System.out.println("cron：" + cronExpression);

        // 计算接下来的几个时间
        System.out.println("========================= hutool 工具");
        try {
            CronUtil.matchedDates(cronExpression, 5, new Date())
                    .forEach(date -> System.out.printf("%tF %<tT%n", date));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("========================= spring 工具");
        try {
            CronUtil.getNextTimes(cronExpression, 5)
                    .forEach(date -> System.out.printf("%tF %<tT%n", date));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
