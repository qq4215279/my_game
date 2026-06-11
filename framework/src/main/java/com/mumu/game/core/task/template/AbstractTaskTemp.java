package com.mumu.game.core.task.template;

import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;
import com.mumu.game.business.player.domain.Player;
import com.mumu.game.business.task.consts.TaskConstants;
import com.mumu.game.business.task.dao.PlayerTaskOperator;
import com.mumu.game.business.task.luban.TaskConfigManager;
import com.mumu.game.business.task.luban.dto.TaskConfigDTO;
import com.mumu.game.business.task.model.PlayerTask;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.log.LogAction;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.task.BaseTaskBean;
import com.mumu.game.proto.task.TaskBean;
import com.mumu.game.template.func.enums.ResetEnum;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractTaskTemp
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:04
 */
public abstract class AbstractTaskTemp implements TaskTemp {
    protected static final LogTopic log = LogTopic.ACTION;

    /** 一键领取所有任务奖励 */
    protected static final int ONE_KEY_FLAG = -1;

    /** 任务类型 */
    @Setter
    protected int taskType;

    @Resource
    protected PlayerManager playerManager;

    @Resource
    PlayerTaskOperator playerTaskOperator;

    @Override
    public void checkRefreshTask(long playerId) {
        List<Integer> tmpTaskIdList = new ArrayList<>(TaskConfigManager.getTaskIdList(taskType));
        // 获取玩家指定类型所有任务
        // 1. 检查玩家任务DO，移除无效的任务数据
        List<PlayerTask> PlayerTaskList =
                playerTaskOperator.getPlayerTaskDOList(playerId, taskType);
        for (PlayerTask PlayerTask : PlayerTaskList) {
            int taskId = PlayerTask.getTaskId();
            TaskConfigDTO taskConfig = TaskConfigManager.getTask(taskId);
            // 无配置 || 根任务无法找到，移除任务DO
            if (taskConfig == null
                    || tmpTaskIdList.isEmpty()
                    || !tmpTaskIdList.contains(taskConfig.getRootTaskId())) {
                playerTaskOperator.delete(PlayerTask);
                continue;
            }

            checkTaskState(PlayerTask, taskConfig);

            // 移除已存在的根任务
            tmpTaskIdList.remove((Integer) taskConfig.getRootTaskId());

            // 检查配置开关，刷新无效DO，若开关开，则根据完成情况尝试开启后续任务
            if (checkSwitchAndRefreshTaskDO(taskConfig, PlayerTask)) {
                tryStartNextTask(PlayerTask);
            }
        }

        // 2. 添加变更后新的任务
        tmpTaskIdList.forEach(
                taskId -> {
                    // 获取第一个有效任务
                    TaskConfigDTO conf = TaskConfigManager.getTask(taskId);
                    if (conf != null) {
                        insertAndUpdateProgressTask(playerId, conf, 0);
                    } else {
                        LogTopic.ACTION.error(
                                LogAction.ACTIVITY_TASK,
                                "refreshTask 配置的初始任务不存在",
                                "playerId",
                                playerId,
                                "taskType",
                                taskType,
                                "taskId",
                                taskId);
                    }
                });
    }

    private void checkTaskState(PlayerTask PlayerTask, TaskConfigDTO taskConfig) {
        long playerId = PlayerTask.getPlayerId();
        int taskId = taskConfig.getTaskId();

        // 任务进度变更，将完成状态变更
        if (PlayerTask.getState() == TaskConstants.FINISH_STATE
                && PlayerTask.getProgress() < taskConfig.getTargetNum()) {
            PlayerTask.setState(TaskConstants.UN_FINISH_STATE);
            playerTaskOperator.update(PlayerTask);

            // 发布任务状态变更事件
            SpringContextUtils.publishEvent(
                    TaskStateChangeEvent.of(
                            playerManager.getPlayer(playerId), taskType, taskId, PlayerTask.getState()));
        }

        if (PlayerTask.getState() == TaskConstants.UN_FINISH_STATE
                && PlayerTask.getProgress() >= taskConfig.getTargetNum()) {
            PlayerTask.setState(TaskConstants.FINISH_STATE);
            playerTaskOperator.update(PlayerTask);

            // 发布任务状态变更事件
            SpringContextUtils.publishEvent(
                    TaskStateChangeEvent.of(
                            playerManager.getPlayer(playerId), taskType, taskId, PlayerTask.getState()));
        }
    }

    /**
     * 插入任务
     *
     * @param playerId playerId
     * @param taskConfig taskConfig
     * @param progress progress
     * @since 2025/4/16 17:56
     */
    private void insertAndUpdateProgressTask(long playerId, TaskConfigDTO taskConfig, long progress) {
        int actionType = taskConfig.getActionType();
        int taskId = taskConfig.getTaskId();
        long newProgress = progress;
        int state = TaskConstants.UN_FINISH_STATE;
        try {
            ActionType actionTypeEnum = ActionType.getActionType(actionType);
            newProgress = actionTypeEnum.updateTaskProgress(playerId, progress);
            state =
                    newProgress >= taskConfig.getTargetNum()
                            ? TaskConstants.FINISH_STATE
                            : TaskConstants.UN_FINISH_STATE;
        } catch (Exception e) {
            log.error(
                    e,
                    "insertTask.triggerUpdateTask",
                    "error",
                    "actionType",
                    actionType,
                    "playerId",
                    playerId,
                    "taskType",
                    taskType,
                    "taskId",
                    taskId,
                    "progress",
                    progress,
                    "newProgress",
                    newProgress);
        }

        playerTaskOperator.insertTask(playerId, taskType, taskId, newProgress, state);
    }

    /** 检查任务配置开关，并刷新玩家DO true-开关开，无需刷新DO */
    private boolean checkSwitchAndRefreshTaskDO(TaskConfigDTO taskConfig, PlayerTask taskDO) {
        // 任务开，无需操作
        if (taskConfig.isOnOff()) return true;

        // 1.若存在下一个有效任务，则更新任务DO为下一个任务
        TaskConfigDTO nextTaskDTO = TaskConfigManager.getNextTask(taskDO.getTaskId());
        if (nextTaskDTO != null) {
            insertAndUpdateProgressTask(
                    taskDO.getPlayerId(),
                    nextTaskDTO,
                    nextTaskDTO.isExtendFrontTaskProgress() ? taskDO.getProgress() : 0);

        } else {
            // 2.若无后续任务，则找前一个有效任务，若存在，更新DO
            TaskConfigDTO preTaskDTO = TaskConfigManager.getPreTask(taskDO.getTaskId());
            if (preTaskDTO != null) {
                insertAndUpdateProgressTask(taskDO.getPlayerId(), preTaskDTO, preTaskDTO.getTargetNum());
            }
        }

        playerTaskOperator.delete(taskDO);
        return false;
    }

    /** 开始下一个任务 true-有新任务 false-当前任务未完成或无后续任务 */
    private void tryStartNextTask(PlayerTask taskDO) {
        if (taskDO.getState() == TaskConstants.RECEIVE_STATE) return;

        TaskConfigDTO nextTaskDTO = TaskConfigManager.getNextTask(taskDO.getTaskId());
        // 若有后续任务，更新任务DO
        if (nextTaskDTO != null) {
            insertAndUpdateProgressTask(
                    taskDO.getPlayerId(),
                    nextTaskDTO,
                    nextTaskDTO.isExtendFrontTaskProgress() ? taskDO.getProgress() : 0);

            playerTaskOperator.delete(taskDO);
        }
    }

    @Override
    public void updateProgress(Player player, ActionData data) {
        long playerId = player.getPlayerId();
        List<PlayerTask> PlayerTaskList =
                playerTaskOperator.getPlayerTaskList(playerId, taskType);

        boolean isRedDot = false;
        for (PlayerTask PlayerTask : PlayerTaskList) {
            if (doUpdateProgress(player, PlayerTask, data)) isRedDot = true;
        }

        // 红点触发功能模块红点推送
        if (isRedDot) {
            Template template = TaskConfigManager.getTemplateByTaskType(player.getPlayerId(), taskType);
            if (template.isOpen(playerId)) template.pushFunctionStateMessage(playerId);
        }
    }

    /**
     * 更新任务进度
     *
     * @param player player
     * @param PlayerTask PlayerTask
     * @param data data
     * @since 2024/12/13 13:45
     */
    protected boolean doUpdateProgress(Player player, PlayerTask PlayerTask, ActionData data) {
        ActionType actionType = data.getType();
        TaskConfigDTO taskConfig = TaskConfigManager.getTask(PlayerTask.getTaskId());
        // 不是目标类别 || 任务已领取 || 任务进度已达上限
        if (taskConfig.getActionType() != actionType.getType()
                || PlayerTask.getState() == TaskConstants.RECEIVE_STATE
                || PlayerTask.getProgress() >= taskConfig.getTargetNum()) {
            return false;
        }

        // 计算任务进度
        long beforeProgress = PlayerTask.getProgress();
        Long progress = data.updateProgress(player, PlayerTask.getProgress(), taskConfig);
        if (progress == null) {
            return false;
        }

        PlayerTask.setProgress(Math.min(progress, taskConfig.getTargetNum()));
        playerTaskOperator.update(PlayerTask);

        boolean isRedDot = false;
        // 任务完成红点提示
        if (progress >= taskConfig.getTargetNum()) {
            // 是否红点：有奖励才标记红点
            isRedDot = StringUtils.isNotBlank(taskConfig.getRewards());
            // 任务状态：有奖励标记为 已完成，无奖励标记为 已领取
            int state =
                    StringUtils.isNotBlank(taskConfig.getRewards())
                            ? TaskConstants.FINISH_STATE
                            : TaskConstants.RECEIVE_STATE;
            PlayerTask.setState(state);
            playerTaskOperator.update(PlayerTask);

            // 触发action
            ResetEnum resetEnum = TaskConfigManager.getTaskTypeReset(PlayerTask.getTaskType());
            if (resetEnum != null) {
                resetEnum.triggerTaskAction(player);
            }

            // 发布任务状态变更事件
            SpringContextUtils.publishEvent(
                    TaskStateChangeEvent.of(
                            player, taskType, PlayerTask.getTaskId(), PlayerTask.getState()));

            // 其他，发布任务进度事件
        } else {
            SpringContextUtils.publishEvent(
                    TaskProgressChangeEvent.of(player, taskType, PlayerTask.getTaskId()));
        }

        // 任务进度更新日志
        LogTopic.ACTION.debug(
                player,
                LogAction.ACTIVITY_TASK,
                "actionType",
                actionType,
                "taskId",
                PlayerTask.getTaskId(),
                "taskType",
                PlayerTask.getTaskType(),
                "resetType",
                TaskConfigManager.getTaskTypeResetValue(taskType),
                "taskDesc",
                taskConfig.getDesc(),
                "beforeProgress",
                beforeProgress,
                "afterProgress",
                progress,
                "isRedDot",
                isRedDot,
                "data",
                data);
        return isRedDot;
    }

    @Override
    public boolean checkRedPoint(long playerId) {
        return doCheckRedPoint(playerId, taskType);
    }

    /** 默认的检查任务红点工具方法 */
    protected boolean doCheckRedPoint(long playerId, int taskType) {
        List<PlayerTask> PlayerTaskList =
                playerTaskOperator.getPlayerTaskList(playerId, taskType);
        for (PlayerTask PlayerTask : PlayerTaskList) {
            // 有未领奖的任务
            if (PlayerTask.getState() == TaskConstants.FINISH_STATE) {
                return true;
            }
        }
        return false;
    }

    /** 校验能否领取任务奖励 */
    protected ErrorCode checkTaskReward(long playerId, int taskId) {
        return ErrorCode.SUCCESS;
    }

    /**
     * 获取能领取领奖的任务DO
     *
     * @param playerId playerId
     * @param taskId 任务id -1领取全部
     * @return key-状态 value-任务DOs
     * @since 2024/12/6 14:24
     */
    protected Pair<ErrorCode, List<PlayerTask>> findCanRewardTaskDO(long playerId, int taskId) {
        List<Integer> taskIdList = TaskConfigManager.getTaskIdList(taskType);
        if (taskIdList.isEmpty()) {
            return Pair.of(ErrorCode.FAIL_PARAM_ERROR, Collections.emptyList());
        }

        // 功能校验
        // Template template = TaskConfigManager.getTemplateByTaskType(playerId, taskType);
        // if (!template.isOpen(playerId)) {
        //   return Pair.of(ErrorCode.FAIL_FUNCTION_NOT_OPOEN, Collections.emptyList());
        // }
        // ErrorCode errorCode = template.checkTaskReward(taskType, taskId);
        ErrorCode errorCode = checkTaskReward(playerId, taskId);
        if (errorCode != ErrorCode.SUCCESS) {
            return Pair.of(errorCode, Collections.emptyList());
        }

        List<PlayerTask> rewardDOList = new ArrayList<>();
        // 一键领取所有任务奖励
        if (taskId == ONE_KEY_FLAG) {
            List<PlayerTask> PlayerTaskList =
                    playerTaskOperator.getPlayerTaskList(playerId, taskType).stream()
                            .filter(o -> TaskConfigManager.getTask(o.getTaskId()) != null)
                            .filter(o -> o.getState() == TaskConstants.FINISH_STATE)
                            .toList();
            rewardDOList.addAll(PlayerTaskList);

            // 领取单条任务奖励
        } else {
            TaskConfigDTO taskDTO = TaskConfigManager.getTask(taskId);
            if (taskDTO == null) {
                return Pair.of(ErrorCode.FAIL_PARAM_ERROR, Collections.emptyList());
            }

            PlayerTask PlayerTask = playerTaskOperator.getPlayerTask(playerId, taskType, taskId);
            if (PlayerTask == null) {
                return Pair.of(ErrorCode.FAIL_PARAM_ERROR, Collections.emptyList());
            }
            // 未完成
            if (PlayerTask.getState() == TaskConstants.UN_FINISH_STATE) {
                return Pair.of(ErrorCode.FAIL_TASK_NO_FINISH, Collections.emptyList());
            }
            // 已领奖
            if (PlayerTask.getState() == TaskConstants.RECEIVE_STATE) {
                return Pair.of(ErrorCode.FAIL_HAS_RECEIVE_TASK_REWARD, Collections.emptyList());
            }

            rewardDOList.add(PlayerTask);
        }

        if (rewardDOList.isEmpty()) {
            return Pair.of(ErrorCode.FAIL_NO_TASK_REWARD, Collections.emptyList());
        }

        return Pair.of(ErrorCode.SUCCESS, rewardDOList);
    }

    @Override
    public Pair<ErrorCode, List<ItemBean>> getTaskReward(long playerId, Integer taskId) {
        if (taskId == null) {
            taskId = ONE_KEY_FLAG;
        }

        // 任务领取校验
        Pair<ErrorCode, List<PlayerTask>> pair = findCanRewardTaskDO(playerId, taskId);
        ErrorCode errorCode = pair.getKey();
        if (errorCode != ErrorCode.SUCCESS) {
            return Pair.of(errorCode, Collections.emptyList());
        }

        List<PlayerTask> rewardDOList = pair.getValue();
        // 标记领奖
        Map<Integer, String> taskIdRewardMap = getRewardsAndUpdateStatus(rewardDOList);
        if (taskIdRewardMap.isEmpty()) {
            return Pair.of(ErrorCode.FAIL_NO_TASK_REWARD, Collections.emptyList());
        }

        // 功能模版
        Template template = TaskConfigManager.getTemplateByTaskType(playerId, taskType);

        List<ItemBean> itemBeans = Lists.newArrayList();
        for (Map.Entry<Integer, String> entry : taskIdRewardMap.entrySet()) {
            // 合并奖励并掉落
            Drop drop = Drop.of(entry.getValue());
            DropParams dropParams =
                    DropParams.build()
                            .setFunctionId(template.getFunctionId())
                            .setConfigName("ConfigTask")
                            .setConfigId(entry.getKey());
            itemBeans.addAll(drop.rewardItem(playerId, CurrencyAction.TASK, dropParams));
        }

        // 功能模版处理after reward
        // template.handleAfterTaskReward(taskType, taskId, resMsg);

        // 红点校验
        template.pushFunctionStateMessage(playerId);

        return Pair.of(ErrorCode.SUCCESS, itemBeans);
    }

    /** 根据玩家任务完成情况，获取任务奖励（只有任务完成且未领取时，才会返回任务奖励） */
    private Map<Integer, String> getRewardsAndUpdateStatus(List<PlayerTask> taskDOs) {

        Map<Integer, String> taskIdRewardMap = new HashMap<>();
        for (PlayerTask PlayerTask : taskDOs) {
            String reward = getRewardsAndUpdateStatus(PlayerTask);
            if (StringUtils.isEmpty(reward)) {
                continue;
            }

            taskIdRewardMap.put(PlayerTask.getTaskId(), reward);
        }
        return taskIdRewardMap;

    /*return taskDOs.stream()
    .map(this::getRewardsAndUpdateStatus)
    .filter(StringUtils::isNotBlank)
    .collect(Collectors.joining(SymbolConstants.SEMICOLON));*/
    }

    /** 根据玩家任务完成情况，获取任务奖励，同时更新任务状态 */
    private String getRewardsAndUpdateStatus(PlayerTask taskDO) {
        TaskConfigDTO taskDTO = TaskConfigManager.getTask(taskDO.getTaskId());

        // 更新任务领奖状态
        taskDO.setState(TaskConstants.RECEIVE_STATE);
        playerTaskOperator.update(taskDO);

        // 发布任务状态变更事件
        SpringContextUtils.publishEvent(
                TaskStateChangeEvent.of(
                        playerManager.getPlayer(taskDO.getPlayerId()),
                        taskType,
                        taskDO.getTaskId(),
                        taskDO.getState()));

        // 尝试开始后续任务
        tryStartNextTask(taskDO);

        return taskDTO.getRewards();
    }

    @Override
    public List<TaskBean> buildTaskBeanList(long playerId) {
        List<TaskBean> beanList = new ArrayList<>();
        List<PlayerTask> PlayerTaskList =
                playerTaskOperator.getPlayerTaskList(playerId, taskType);
        for (PlayerTask PlayerTask : PlayerTaskList) {
            TaskBean taskBean = buildClientTask(PlayerTask);
            if (taskBean != null) beanList.add(taskBean);
        }

        return beanList;
    }

    /** 构造客户端任务信息 */
    public TaskBean buildClientTask(PlayerTask taskDO) {
        TaskBean bean = new TaskBean();
        BaseTaskBean baseTaskBean = TaskConfigManager.getTask(taskDO.getTaskId()).toBean();
        baseTaskBean.setTaskType(taskType);
        baseTaskBean.setResetType(TaskConfigManager.getTaskTypeResetValue(taskType));
        baseTaskBean.setCurrProgress(taskDO.getProgress());
        baseTaskBean.setState(taskDO.getState());
        bean.setBaseTaskBean(baseTaskBean);
        return bean;
    }
}
