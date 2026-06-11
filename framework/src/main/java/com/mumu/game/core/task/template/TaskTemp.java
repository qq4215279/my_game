package com.mumu.game.core.task.template;

import cn.hutool.core.lang.Pair;
import com.mumu.game.business.player.domain.Player;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.task.TaskBean;

import java.util.List;

/**
 * TaskTemp
 * 任务模版
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:03
 */
public interface TaskTemp {
    /** 当前任务类型是否解锁 */
    default boolean isOpen(long playerId) {
        return true;
    }

    /**
     * 检查并刷新任务
     * @param playerId playerId
     * @since 2024/12/4 11:39
     */
    void checkRefreshTask(long playerId);

    /**
     *更新任务进度
     * @param player player
     * @param data data
     * @since 2024/12/13 11:28
     */
    void updateProgress(Player player, ActionData data);

    /**
     * 检查任务红点
     * @param playerId playerId
     * @return boolean
     * @since 2024/12/4 11:50
     */
    boolean checkRedPoint(long playerId);

    /**
     * 领取任务奖励
     * @param playerId playerId
     * @param taskId 任务id: null 或 -1 表示领取全部
     * @return com.game.framework.core.cmd.response.ResponseResult
     * @since 2024/12/4 17:38
     */
    Pair<ErrorCode, List<ItemBean>> getTaskReward(long playerId, Integer taskId);

    /**
     * 构建任务bean信息
     * @param playerId playerId
     * @return java.util.List<com.game.proto.task.ClientTaskBean>
     * @since 2024/12/4 11:41
     */
    List<TaskBean> buildTaskBeanList(long playerId);

    /**
     * 构造客户端任务信息
     * @param taskDO taskDO
     * @return com.game.proto.task.TaskBean
     * @since 2025/4/2 14:50
     */
    TaskBean buildClientTask(PlayerTaskDO taskDO);
}
