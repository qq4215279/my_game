package com.mumu.framework.business.task.enums;

import com.mumu.framework.business.player.domain.Player;
import java.util.Map;

/**
 * enumsOperationEnum
 * 操作类型
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:18
 */
public enum OperationEnum {
  /** 更换头像框 */
  CHANGE_HEAD_FRAME(1),
  /** 更换面板皮肤 */
  CHANGE_PANEL_SKIN(2),
  /** 更换牌背皮肤 */
  CHANGE_POKER_SKIN(3),
  /** 更换桌背皮肤 */
  CHANGE_TABLE_SKIN(4),
  /** 更换气泡框 */
  CHANGE_bubble_Frame(5),
  /** 添加好友 */
  ADD_FRIEND(6),
  /** 完成日常任务 */
  FINISH_DAILY_TASK(7),
  /** 完成周常任务 */
  FINISH_WEEKLY_TASK(8),
  /** 完成月常任务 */
  FINISH_MONTH_TASK(9),
  /** 查看自建房 */
  SELECT_DIY_TABLES(10),
  /** 绑定Facebook账号 */
  BIND_FACEBOOK(11),
  ;

  /** 操作类型 */
  private final int type;

  OperationEnum(int type) {
    this.type = type;
  }

  /** 触发一次操作 */
  public void trigger(long playerId) {
    trigger(playerId, 1);
  }

  public void trigger(long playerId, long count) {
    // ActionExecutor.action(
    //     playerId, ActionData.of(ActionType.OPERATION, 0L, null, Map.of(type, count)));
  }

  public void trigger(Player player) {
    trigger(player, 1);
  }

  /** 触发一次操作 */
  public void trigger(Player player, long count) {
    // ActionExecutor.action(player, ActionType.OPERATION, Map.of(type, count));
  }
}
