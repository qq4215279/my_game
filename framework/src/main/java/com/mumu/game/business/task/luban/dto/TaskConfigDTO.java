package com.mumu.game.business.task.luban.dto;

import java.util.List;

import com.game.business.item.reward.drop.Drop;
import com.game.business.task.progress.IActionTaskConfig;
import com.game.consts.Symbol;
import com.game.framework.core.utils.CovertUtil;
import com.game.luban.hall.task.ConfigTask;
import com.game.proto.task.BaseTaskBean;

import lombok.Getter;
import lombok.Setter;

/** 任务配置信息 @Date: 2024/11/20 下午5:04 @Author: xu.hai */
@Getter
public class TaskConfigDTO implements IActionTaskConfig {
  /** 任务ID */
  private final int taskId;

  /** 任务描述 */
  private final String desc;

  /** 游戏列表 */
  private final List<Integer> gameIds;

  /** 房间列表 */
  private final List<Integer> roomIds;

  /** 行为类别 */
  private final int actionType;

  /** 目标条件 */
  private final List<Long> condition;

  /** 目标数量 */
  private final long targetNum;

  /** 奖励信息 */
  private final String rewards;

  /** 跳转标识 */
  private final String guide;

  /** 任务图标 */
  private final String icon;

  /** 开关 */
  private final boolean onOff;

  /** 前置任务ID */
  private final int frontTaskId;

  /** 继承进度 */
  private final boolean extendFrontTaskProgress;

  /** 后续任务 */
  @Setter private int nextTaskId;

  /** 根任务 - 方便快速定位当前任务所属根 */
  @Setter private int rootTaskId;

  public TaskConfigDTO(ConfigTask conf) {
    this.taskId = Integer.parseInt(conf.getData_id());
    this.desc = conf.getDesc();
    this.gameIds = CovertUtil.stringToIntList(conf.getGameIds(), Symbol.COMMA);
    this.roomIds = CovertUtil.stringToIntList(conf.getRoomIds(), Symbol.COMMA);
    this.actionType = conf.getActionType();
    this.condition =
        CovertUtil.stringToList(conf.getCondition(), Symbol.COMMA, Long.class);
    this.targetNum = conf.getTargetNum();
    this.rewards = conf.getReward();
    this.guide = conf.getGuide();
    this.icon = conf.getIcon();
    this.onOff = conf.getOn_off();
    this.frontTaskId = conf.getFrontTaskId();
    this.extendFrontTaskProgress = conf.getExtendFrontTaskProgress();
  }

  public BaseTaskBean toBean() {
    BaseTaskBean bean = new BaseTaskBean();
    bean.setTaskId(taskId);
    bean.setDesc(desc);
    bean.setTargetNum(targetNum);
    bean.setRewards(Drop.of(rewards).buildItemBeans());
    bean.setGuide(guide);
    bean.setIcon(icon);
    return bean;
  }

  @Override
  public int actionType() {
    return actionType;
  }

  @Override
  public List<Integer> taskGameIds() {
    return gameIds;
  }

  @Override
  public List<Integer> taskRoomIds() {
    return roomIds;
  }

  @Override
  public List<Long> taskCondition() {
    return condition;
  }

  @Override
  public long taskTarget() {
    return targetNum;
  }
}
