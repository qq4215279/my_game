package com.mumu.game.core.task;

import cn.hutool.core.collection.CollUtil;
import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.AutoInitManager;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.task.anno.TaskType;
import com.mumu.game.core.task.template.AbstractTaskTemp;
import com.mumu.game.core.task.template.DefaultTaskTemp;
import com.mumu.game.core.task.template.TaskTemp;
import com.mumu.game.core.utils.ModifierUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TaskTemplateManager
 * 任务模版管理器
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:05
 */
@Component
public class TaskTemplateManager implements AutoInitEvent, AutoLubanEvent<TaskConfigLoader> {

    private static final Map<Integer, Class<? extends AbstractTaskTemp>> TASK_TYPE_CLAZZ_MAP =
            new HashMap<>();

    /** 任务类型 与 任务模版 映射 */
    private static Map<Integer, TaskTemp> taskTypeTemplateMap = Collections.emptyMap();

    /** 任务类型对应的解锁条件类型 */
    private static Map<Integer, Integer> taskTypeLockTypeMap = Collections.emptyMap();

    @Override
    public void autoInit() {
        findTypeTemplate();
        initTemplate();
    }

    @Override
    public void autoLubanRefresh() {
        initTemplate();
    }

    /** 查找任务类型模板 */
    private static void findTypeTemplate() {
        for (Class<?> c : AutoInitManager.CLASSES) {
            TaskType annotation = c.getAnnotation(TaskType.class);
            if (annotation != null && ModifierUtil.isBelongTo(c, AbstractTaskTemp.class)) {
                for (int typeId : annotation.value()) {
                    TASK_TYPE_CLAZZ_MAP.put(typeId, (Class<? extends AbstractTaskTemp>) c);
                }
            }
        }
    }

    /**
     * 初始化任务类型模板
     *
     * @since 2024/12/11 10:04
     */
    private void initTemplate() {
        Map<String, ConfigTaskType> typeMap = getLubanLoader().getConfigTaskTypeMap();
        if (CollUtil.isEmpty(typeMap)) return;

        // 提前解析任务类型对应的解锁条件类型
        taskTypeLockTypeMap =
                typeMap.values().stream()
                        .filter(c -> StringUtils.isNotBlank(c.getLockConditionType()))
                        .collect(
                                Collectors.toMap(
                                        c -> Integer.parseInt(c.getData_id()),
                                        c -> Integer.parseInt(c.getLockConditionType())));

        // 初始化任务模板实现
        Map<Integer, TaskTemp> tmpTaskTypeTemplateMap = new HashMap<>();
        for (ConfigTaskType typeConf : typeMap.values()) {
            int taskType = Integer.parseInt(typeConf.getData_id());
            AbstractTaskTemp taskTemplate = SpringContextUtils.getBean(getTemplateClazz(taskType));
            taskTemplate.setTaskType(taskType);

            tmpTaskTypeTemplateMap.put(taskType, taskTemplate);
        }
        taskTypeTemplateMap = tmpTaskTypeTemplateMap;
    }

    private Class<? extends AbstractTaskTemp> getTemplateClazz(int taskType) {
        Class<? extends AbstractTaskTemp> tmpClass = TASK_TYPE_CLAZZ_MAP.get(taskType);
        if (tmpClass != null) return tmpClass;
        // 锁定任务的默认实现
        if (taskTypeLockTypeMap.containsKey(taskType)) return DefaultLockTaskTemp.class;
        // 解锁条件任务的默认实现
        if (taskTypeLockTypeMap.containsValue(taskType)) return DefaultTaskConditionTemp.class;
        // 默认任务模版
        return DefaultTaskTemp.class;
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.COMMON;
    }

    /**
     * 获取任务模版
     *
     * @param taskType 任务类型
     * @return com.game.template.task.core.template.TaskTemplate
     * @since 2024/12/4 15:13
     */
    public static TaskTemp getTaskTemplate(int taskType) {
        return taskTypeTemplateMap.get(taskType);
    }

    /**
     * 获取任务模版列表
     *
     * @param functionId 功能id（一个功能下存在多个任务类型）
     * @return java.util.List<com.game.template.task.core.template.TaskTemplate>
     * @since 2024/12/4 15:13
     */
    public static List<TaskTemp> getTaskTemplateList(int functionId) {
        return TaskConfigManager.getTaskTypeListByFuncId(functionId).stream()
                .map(TaskTemplateManager::getTaskTemplate)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 获取任务的解锁条件任务类型 */
    public static int getTaskLockType(int taskType) {
        return taskTypeLockTypeMap.getOrDefault(taskType, 0);
    }
}

