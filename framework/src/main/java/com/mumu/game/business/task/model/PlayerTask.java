package com.mumu.game.business.task.model;

import lombok.Data;

/**
 * PlayerTask
 * 玩家任务表
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:07
 */
@Data
public class PlayerTask {

    /** 玩家id */
    private long playerId;
    /** 任务类型 */
    private int taskType;
    /** 任务id */
    private int taskId;
    /** 任务进度 */
    private long progress;
    /** 任务状态(0: 已完成(未领取); 1:未完成; 2: 已领取) */
    private int state;
}
