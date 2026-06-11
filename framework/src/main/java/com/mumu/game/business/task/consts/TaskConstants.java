package com.mumu.game.business.task.consts;

/**
 * TaskConstants
 * 任务常量类
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:07
 */
public interface TaskConstants {
    // 任务状态
    /** 任务状态 - 已完成（未领奖） */
    int FINISH_STATE = 0;
    /** 任务状态 - 未完成 */
    int UN_FINISH_STATE = 1;
    /** 任务状态 - 已领取 */
    int RECEIVE_STATE = 2;
    /** 任务状态 - 未解锁 */
    int LOCK_STATE = 3;
}
