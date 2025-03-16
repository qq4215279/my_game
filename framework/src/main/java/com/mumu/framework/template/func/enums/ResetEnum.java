package com.mumu.framework.template.func.enums;

import com.mumu.framework.business.player.domain.Player;
import com.mumu.framework.business.task.enums.OperationEnum;
import com.mumu.framework.util2.DateUtil;
import com.mumu.framework.util2.WeekUtil;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    public boolean neetReset(long lastResetTime) {
      return !DateUtil.isSameDay(lastResetTime);
    }

    @Override
    public void triggerTaskAction(Player player) {
      OperationEnum.FINISH_DAILY_TASK.trigger(player);
    }
  },
  /** 每周 */
  WEEKLY(2) {
    @Override
    public boolean neetReset(long lastResetTime) {
      return !WeekUtil.isSameWeek(lastResetTime);
    }
    @Override
    public void triggerTaskAction(Player player) {
      OperationEnum.FINISH_WEEKLY_TASK.trigger(player);
    }
  },
  /** 每月 */
  MONTH(3){
    @Override
    public boolean neetReset(long lastResetTime) {
      return !DateUtil.isSameMonth(lastResetTime);
    }
    @Override
    public void triggerTaskAction(Player player) {
      OperationEnum.FINISH_MONTH_TASK.trigger(player);
    }
  },
  /** 赛季 */
  SEASON(4),
  /** 永久限购(终生) */
  ALL_LIFE(5),
  ;

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
  public boolean neetReset(long lastResetTime) {
    return false;
  }

  /**
   * 触发完成重置类型任务action
   * @date 2025/3/6 21:39
   */
  public void triggerTaskAction(Player player) {
  }
}
