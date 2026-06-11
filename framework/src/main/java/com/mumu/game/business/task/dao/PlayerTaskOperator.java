package com.mumu.game.business.task.dao;

import com.mumu.game.business.task.model.PlayerTask;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * PlayerTaskOperator
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:09
 */
@Component
public class PlayerTaskOperator {

    public PlayerTask getPlayerTaskDO(long playerId, int taskType, int taskId) {
        return null;
    }

    /** 获取指定类型的全部任务 */
    public List<PlayerTask> getPlayerTaskDOList(long playerId) {
        return Collections.emptyList();
    }

    /** 获取指定类型的全部任务 */
    public List<PlayerTask> getPlayerTaskDOList(long playerId, int taskType) {
        // return selectList(playerId, m -> m.getTaskType() == taskType);
        return Collections.emptyList();
    }

    /** 新增一个任务 */
    public void insertTask(long playerId, int taskType, int taskId, long progress, int state) {
        PlayerTask taskDO = new PlayerTask();
        taskDO.setPlayerId(playerId);
        taskDO.setTaskType(taskType);
        taskDO.setTaskId(taskId);
        taskDO.setProgress(progress);
        // taskDO.setState(TaskConstants.UN_FINISH_STATE);
        taskDO.setState(state);
        // insert(taskDO);
    }
}
