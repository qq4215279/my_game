/*
 * Copyright 2020-2026, mumu without 996. All Right Reserved.
 */

package com.mumu.game.template.func.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.core.utils.DateUtil;
import com.mumu.game.core.utils.WeekUtil;

import lombok.Getter;

/**
 * ResetEnum
 * 重置类型枚举
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:15
 */
@Getter
public enum ResetEnum {
    /** 无限购买/终生不重置 */
    INFINITE(0),
    /** 每天 */
    DAILY(1) {
        @Override
        public boolean needReset(long lastResetTime) {
            return !DateUtil.isSameDay(lastResetTime);
        }
    },
    /** 每周 */
    WEEKLY(2) {
        @Override
        public boolean needReset(long lastResetTime) {
            return !WeekUtil.isSameWeek(lastResetTime);
        }
    },
    /** 每月 */
    MONTH(3) {
        @Override
        public boolean needReset(long lastResetTime) {
            return !DateUtil.isSameMonth(lastResetTime);
        }
    },
    /** 赛季 */
    SEASON(4),
    /** 永久限购(终生) */
    ALL_LIFE(5),;

    private final int type;

    ResetEnum(int type) {
        this.type = type;
    }

    private static final Map<Integer, ResetEnum> RESET_ENUM_MAP =
        Stream.of(values()).collect(Collectors.toMap(ResetEnum::getType, e -> e));

    public static ResetEnum get(int type) {
        return RESET_ENUM_MAP.get(type);
    }

    /** 需要重置 */
    public boolean needReset(long lastResetTime) {
        return false;
    }

    /**
     * 触发完成重置类型任务action
     * 
     * @since 2025/3/6 21:39
     */
    public void triggerTaskAction(Player player) {}
}
