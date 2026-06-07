package com.mumu.game.business.activity.luban;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mumu.game.luban.config.activity.Activity;

import cn.hutool.core.collection.CollUtil;

/**
 * ActivityConfLuban
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:45
 */
@Component
public class ActivityConfLuban {
    /** 功能id 与 ConfigActivity列表 映射 */
    private static volatile ImmutableMap<Integer, ImmutableList<Activity>> funIdActivityListMap = ImmutableMap.of();

    /** 活动id 与 ConfigActivity 映射 */
    private static volatile ImmutableMap<Integer, Activity> activeIdConfigMap = ImmutableMap.of();

    // @Override
    public void autoLoad() {
        // TODO
        // Collection<Activity> configActivities = getLubanLoader().getConfigPeriodActivityMap().values();
        // funIdActivityListMap = ImmutableUtil.list2ImmMapWithList(configActivities, o -> Integer.parseInt(o.getFunctionId()));
        // activeIdConfigMap = ImmutableUtil.list2ImmMap(configActivities, o -> Integer.parseInt(o.getData_id()));
    }

    /**
     * 活动是否正在开启
     * @param functionId functionId
     * @return boolean
     * @since 2025/6/16 20:11
     */
    public static boolean hasActivity(int functionId) {
        // 非周期性活动 | 正在开启周期性活动
        return !isPeriodActivity(functionId) || findCurrActivityByFunId(functionId) != null;
    }

    /**
     * 获取玩家正在赛季中的活动
     *
     * @param functionId 功能id
     * @return com.game.luban.system.function.ConfigActivity
     * @since 2024/12/4 20:00
     */
    public static Activity findCurrActivityByFunId(int functionId) {
        ImmutableList<Activity> confList = funIdActivityListMap.get(functionId);
        if (CollUtil.isEmpty(confList)) return null;

        long now = System.currentTimeMillis();
        long nowTruncatedToMinute  = getTruncatedToMinute(now);
        Activity res = null;
        for (Activity configPeriodActivity : confList) {
            // 在活动开放时间区间内
            if (now >= configPeriodActivity.startTime && nowTruncatedToMinute <= configPeriodActivity.endTime) {
                res = configPeriodActivity;
            }
        }
        return res;
    }

    /**
     * 获取当前时间戳（向下取整到分钟开始的时间戳）
     * @return long
     * @since 2025/6/16 21:01
     */
    private static long getTruncatedToMinute(long now) {
        // 一分钟的毫秒数
        long oneMinuteMillis = TimeUnit.MINUTES.toMillis(1);
        // 将当前时间向下取整到分钟开始的时间戳
        return now - (now % oneMinuteMillis);
    }

    /**
     * 获取下一个赛季活动
     *
     * @param functionId functionId
     * @return com.game.luban.system.function.ConfigActivity
     * @since 2025/3/5 16:42
     */
    public static Activity getNextConfigActivityByFunctionId(int functionId) {
        long curActivityEndTime = System.currentTimeMillis();
        Activity curConfigPeriodActivity = findCurrActivityByFunId(functionId);
        int curActivityId = 0;
        if (curConfigPeriodActivity != null) {
            curActivityId = curConfigPeriodActivity.id;
            curActivityEndTime = curConfigPeriodActivity.endTime;
        }

        Activity res = null;

        for (Activity nextConfigPeriodActivity :
                funIdActivityListMap.getOrDefault(functionId, ImmutableList.of())) {
            int nextActivityId = nextConfigPeriodActivity.id;
            // 过期id
            if (curActivityId >= nextActivityId) {
                continue;
            }

            // 当前正在开的活动结束时间，晚于开始时间
            if (curActivityEndTime > nextConfigPeriodActivity.startTime) {
                continue;
            }

            // 空 || 找一个更早时间开的活动
            if (res == null || res.startTime >= curActivityEndTime) {
                res = nextConfigPeriodActivity;
            }
        }

        return res;
    }

    /** 是否为周期性功能活动 */
    public static boolean isPeriodActivity(int functionId) {
        return funIdActivityListMap.containsKey(functionId);
    }

    /**
     * getConfigActivity
     *
     * @param activityId 活动id
     * @return com.game.luban.system.function.ConfigActivity
     * @since 2024/12/4 20:01
     */
    public static Activity getConfigActivity(int activityId) {
        return activeIdConfigMap.get(activityId);
    }
}
