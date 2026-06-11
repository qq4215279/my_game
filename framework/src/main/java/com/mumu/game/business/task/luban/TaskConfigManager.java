package com.mumu.game.business.task.luban;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.mumu.game.business.task.luban.dto.TaskConfigDTO;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.CovertUtil;
import com.mumu.game.core.utils.ImmutableUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.template.func.core.temp.Template;
import com.mumu.game.template.func.enums.ResetEnum;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import lombok.Getter;

/** 基础任务配置管理 @Date: 2024/11/20 下午5:01 @Author: xu.hai */
@Component
public class TaskConfigManager implements AutoInitLubanEvent<TaskConfigLoader> {
  public static TaskConfigManager self() {
    return SpringContextUtils.getBean(TaskConfigManager.class);
  }

  /** 任务Map key-任务id */
  @Getter static volatile Map<Integer, TaskConfigDTO> taskMap = Collections.emptyMap();

  /** 功能id 与 任务类型列表 映射 */
  static volatile Map<Integer, List<Integer>> funcIdTaskTypesMap = Collections.emptyMap();

  /** 任务类型 与 功能id 映射 */
  @Getter static volatile Map<Integer, Integer> taskTypeFuncIdMap = Collections.emptyMap();

  /** 任务类型 与 任务id列表 映射 */
  static volatile Map<Integer, List<Integer>> typeTaskIdsMap = Collections.emptyMap();

  /** 任务id 与 任务类型列表 映射 */
  @Getter static volatile Map<Integer, List<Integer>> taskIdTypesMap = Collections.emptyMap();

  /** action 与 任务类型列表 映射 */
  static volatile Map<Integer, Set<Integer>> actionTaskTypesMap = Collections.emptyMap();

  /** 任务类型 与 ConfigTaskType 映射 */
  static ImmutableMap<Integer, ConfigTaskType> idTaskTypeMap;

  @Override
  public void autoLoad() {
    Map<String, ConfigTask> configTaskMap = getLubanLoader().getConfigTaskMap();
    if (CollUtil.isEmpty(configTaskMap)) return;

    Map<Integer, TaskConfigDTO> tmpTaskMap =
        configTaskMap.values().stream()
            .map(TaskConfigDTO::new)
            .collect(Collectors.toMap(TaskConfigDTO::getTaskId, Function.identity()));
    // 构建任务链
    setLinked(tmpTaskMap);

    taskMap = tmpTaskMap;

    Collection<ConfigTaskTab> configTaskTabs = getLubanLoader().getConfigTaskTabMap().values();
    funcIdTaskTypesMap =
        configTaskTabs.stream()
            .collect(
                Collectors.toMap(
                    conf -> Integer.parseInt(conf.getData_id()),
                    conf ->
                        CovertUtil.stringToIntList(conf.getTaskTypes(), Symbol.COMMA)));

    taskTypeFuncIdMap =
        configTaskTabs.stream()
            .flatMap(
                o ->
                    Arrays.stream(o.getTaskTypes().split(Symbol.COMMA))
                        .map(Integer::parseInt)
                        .map(goodsType -> new Pair<>(Integer.parseInt(o.getData_id()), goodsType)))
            .collect(Collectors.toMap(Pair::getValue, Pair::getKey, (o1, o2) -> o2));

    Map<Integer, List<Integer>> tmpTypeTaskIdsMap = new HashMap<>();
    Map<Integer, List<Integer>> tmpTaskIdTypesMap = new HashMap<>();
    Collection<ConfigTaskType> configTaskTypes = getLubanLoader().getConfigTaskTypeMap().values();
    for (ConfigTaskType configTaskType : configTaskTypes) {
      int taskType = Integer.parseInt(configTaskType.getData_id());
      List<Integer> tastIdList =
          configTaskType.getTaskIdList().stream()
              .map(o -> Integer.parseInt(o.getTaskId()))
              .toList();
      for (int taskId : tastIdList) {
        List<Integer> taskIdList =
            tmpTypeTaskIdsMap.computeIfAbsent(taskType, o -> new ArrayList<>());
        taskIdList.add(taskId);

        List<Integer> taskTypesList =
            tmpTaskIdTypesMap.computeIfAbsent(taskId, o -> new ArrayList<>());
        taskTypesList.add(taskType);
      }
    }
    typeTaskIdsMap = tmpTypeTaskIdsMap;
    taskIdTypesMap = tmpTaskIdTypesMap;

    actionTaskTypesMap = new HashMap<>();
    tmpTaskMap
        .values()
        .forEach(
            taskConfigDTO -> {
              Set<Integer> taskTypeSet =
                  actionTaskTypesMap.computeIfAbsent(
                      taskConfigDTO.getActionType(), k -> new HashSet<>());
              taskTypeSet.addAll(
                  tmpTaskIdTypesMap.getOrDefault(
                      taskConfigDTO.getTaskId(), Collections.emptyList()));
            });

    idTaskTypeMap =
        ImmutableUtil.list2ImmMap(configTaskTypes, o -> Integer.parseInt(o.getData_id()));
  }

  /** 设置任务关联关系，生成任务链 */
  private void setLinked(Map<Integer, TaskConfigDTO> taskMap) {
    Set<Integer> visited = Sets.newHashSet();
    for (TaskConfigDTO task : taskMap.values()) {
      if (visited.contains(task.getTaskId())) continue;
      visited.add(task.getTaskId());
      // 从当前节点出发，依次设置父任务链
      TaskConfigDTO pre = taskMap.get(task.getFrontTaskId()), curr = task;
      while (pre != null && pre.getNextTaskId() == 0) {
        pre.setNextTaskId(curr.getTaskId());
        curr = pre;
        pre = taskMap.get(pre.getFrontTaskId());
      }
      TaskConfigDTO root = pre == null ? curr : taskMap.get(pre.getRootTaskId());
      if (root == null) {
        LogTopic.ACTION.error("TaskConfigManager 任务配置出现循环", "task", task.getTaskId());
        return;
      }
      if (curr.getFrontTaskId() == 0 && root != curr) {
        LogTopic.ACTION.error(
            "TaskConfigManager 任务配置出现共同父节点",
            "parentTask",
            pre.getTaskId(),
            "subTask",
            curr.getTaskId());
        return;
      }
      // 设置此链路下的根节点 rootTask
      while (curr != null) {
        visited.add(curr.getTaskId());
        curr.setRootTaskId(root.getTaskId());
        curr = taskMap.get(curr.getNextTaskId());
      }
    }
  }

  /** 是否存在此任务配置 */
  public static boolean containsTask(int taskId) {
    return taskMap.containsKey(taskId);
  }

  /** 获取指定任务（任务可能被关闭） */
  public static TaskConfigDTO getTask(int taskId) {
    return taskMap.get(taskId);
  }

  /** 获取指定任务的前一个有效任务（不含自己，可能为null） */
  public static TaskConfigDTO getPreTask(int taskId) {
    return loopTaskConfig(taskId, false, TaskConfigDTO::getFrontTaskId, Objects::nonNull);
  }

  /** 获取指定任务的下一个有效任务（不含自己，可能为null） */
  public static TaskConfigDTO getNextTask(int taskId) {
    return loopTaskConfig(taskId, false, TaskConfigDTO::getNextTaskId, Objects::nonNull);
  }

  /** 获取指定任务所在链的第一个有效任务（包括自己，即自己为第一个有效任务） */
  public static TaskConfigDTO getFirstTask(int taskId) {
    TaskConfigDTO conf = getTask(taskId);
    if (conf == null) return null;
    return loopTaskConfig(
        conf.getRootTaskId(), true, TaskConfigDTO::getNextTaskId, Objects::nonNull);
  }

  /**
   * 获取指定任务链上的另一个有效任务（一个任务链上可能有部分任务节点被关闭）
   *
   * @param taskId 任务链上的某个任务
   * @param isSource 是否包含源任务
   * @param getOtherTaskId 基于指定任务获取另一个任务（如前一个或后一个）
   * @param interrupt 中断条件
   * @return 另一个有效任务
   */
  private static TaskConfigDTO loopTaskConfig(
      int taskId,
      boolean isSource,
      ToIntFunction<TaskConfigDTO> getOtherTaskId,
      Predicate<TaskConfigDTO> interrupt) {
    TaskConfigDTO c = getTask(taskId);
    if (c == null) return null;
    TaskConfigDTO result = isSource && c.isOnOff() ? c : null;
    int num = 100; // 避免死循环
    while (num-- > 0) {
      if (interrupt != null && interrupt.test(result)) break;
      c = getTask(getOtherTaskId.applyAsInt(c));
      if (c == null) break;
      if (c.isOnOff()) result = c;
    }
    return result;
  }

  /**
   * 获取任务类型列表By功能id
   *
   * @param functionId 功能id
   * @return java.util.List<java.lang.Integer>
   * @since 2024/12/3 20:06
   */
  public static List<Integer> getTaskTypeListByFuncId(int functionId) {
    return funcIdTaskTypesMap.getOrDefault(functionId, Collections.emptyList());
  }

  /**
   * 获取功能id
   *
   * @param taskType 任务类型
   * @return int
   * @since 2024/12/3 20:08
   */
  public static int getFunctionId(int taskType) {
    return taskTypeFuncIdMap.getOrDefault(taskType, -1);
  }

  /**
   * 获取任务列表By任务类型
   *
   * @param taskType taskType
   * @return java.util.List<java.lang.Integer>
   * @since 2024/12/4 10:49
   */
  public static List<Integer> getTaskIdList(int taskType) {
    return typeTaskIdsMap.getOrDefault(taskType, Collections.emptyList());
  }

  /** 获取任务类型下指定任务ID的index */
  public static int getTaskIdx(int taskType, int taskId) {
    return ListUtils.indexOf(getTaskIdList(taskType), id -> id == taskId);
  }

  /**
   * 获取任务类型By任务id
   *
   * @param taskId taskId
   * @return java.util.List<java.lang.Integer>
   * @since 2024/12/4 10:49
   */
  public static List<Integer> getTaskTypeList(int taskId) {
    return taskIdTypesMap.getOrDefault(taskId, Collections.emptyList());
  }

  /**
   * 获取任务类型By actionType
   *
   * @param actionType 动作类型
   * @return java.util.Set<java.lang.Integer>
   * @since 2024/12/13 12:00
   */
  public static Set<Integer> getTaskTypeSet(int actionType) {
    return actionTaskTypesMap.getOrDefault(actionType, Collections.emptySet());
  }

  /**
   * getTaskType
   *
   * @param taskType taskType
   * @return com.game.luban.hall.task.ConfigTaskType
   * @since 2024/12/5 14:04
   */
  public static ConfigTaskType getConfigTaskType(int taskType) {
    return idTaskTypeMap.get(taskType);
  }

  /** 获取任务类型的重置类别 */
  public static ResetEnum getTaskTypeReset(int taskType) {
    return ResetEnum.get(getTaskTypeResetValue(taskType));
  }

  public static int getTaskTypeResetValue(int taskType) {
    ConfigTaskType configTaskType = getConfigTaskType(taskType);
    return configTaskType == null ? 0 : Integer.parseInt(configTaskType.getResetType());
  }

  /** 根据任务类型获取模板对象 */
  public static Template getTemplateByTaskType(long playerId, int taskType) {
    return FunctionIdEnum.loadFuncTemplate(playerId, getFunctionId(taskType));
  }
}
