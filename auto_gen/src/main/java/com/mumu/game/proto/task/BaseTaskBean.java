package com.mumu.game.proto.task;

import com.mumu.game.proto.item.ItemBean;
import lombok.Data;

/**
 * BaseTaskBean
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:11
 */
@Data
public class BaseTaskBean {
    /** 任务类型 */
    private Integer taskType;
    /** 任务id */
    private Integer taskId;
    /** 任务重置类型(0: 终生不重置; 1: 每天重置; 2: 每周重置; 3: 每月重置; 4: 赛季重置) */
    private Integer resetType;
    /** 任务描述 */
    private String desc;
    /** 目标数量 */
    private Long targetNum;
    /** 奖励 */
    private java.util.List<ItemBean> rewards = new java.util.ArrayList<>();
    /** 跳转标识 */
    private String guide;
    /** 任务图标 */
    private String icon;
    /** 当前任务进度 */
    private Long currProgress;
    /** 任务状态(0: 已完成(未领取); 1:未完成; 2: 已领取; 3:未解锁) */
    private Integer state;
}
