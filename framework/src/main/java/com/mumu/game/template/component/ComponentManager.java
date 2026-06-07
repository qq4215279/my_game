package com.mumu.game.template.component;

import cn.hutool.core.collection.CollStreamUtil;
import com.google.common.collect.Maps;
import com.mumu.game.business.function.luban.FunctionConfLuban;
import com.mumu.game.business.function.luban.dto.FunctionDTO;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.ModifierUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.template.component.anno.ComponentType;
import com.mumu.game.template.component.core.IComponent;
import com.mumu.game.template.component.enums.ComponentTypeEnum;
import com.mumu.game.template.func.enums.ResetEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ComponentManager
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:18
 */
@Component
public class ComponentManager {
    /** 活动主键集合 k-活动id val-活动组件实现类 */
    private static Map<Integer, IComponent> ACTIVITY_MAP = Collections.emptyMap();

    /** 功能id与活动组件关联map k-funcId val-activityId */
    private static Map<Integer, List<Integer>> functionIdActivityIdsMap = Collections.emptyMap();

    // @Override
    public void autoInit() {
        initActivityMap();
        initConfig();
    }

    // @Override
    public void autoLubanRefresh() {
        initConfig();
    }

    /** 初始化活动组件Map */
    private void initActivityMap() {
        ACTIVITY_MAP =
                CollStreamUtil.toIdentityMap(
                        SpringContextUtils.getBeansOfType(IComponent.class).values(),
                        activity ->
                                activity.getClass().getAnnotation(ComponentType.class).value().getActivityId());
        LogTopic.ACTION.info("ActivityManager.initActivityMap", "activityTypes", ACTIVITY_MAP.keySet());
    }

    /** 刷新功能表配置 */
    private void initConfig() {
        Map<Integer, List<Integer>> map = Maps.newHashMap();
        for (Map.Entry<Integer, FunctionDTO> entry :
                FunctionConfLuban.getFunctionMap().entrySet()) {
            int functionId = entry.getKey();
            FunctionDTO configFunctionDTO = entry.getValue();

            String[] activityType = configFunctionDTO.getActivityType();
            if (activityType == null || activityType.length == 0) {
                continue;
            }
            List<Integer> activityIdList = Arrays.stream(activityType).map(Integer::valueOf).toList();
            map.computeIfAbsent(functionId, k -> new ArrayList<>()).addAll(activityIdList);
        }
        functionIdActivityIdsMap = map;
        LogTopic.ACTION.info(
                "ActivityManager.initFuncIdActivityIdMap",
                "functionIdActivityIdsMap",
                functionIdActivityIdsMap);
    }

    // @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.COMMON;
    }

    /** 排序（小的优先执行） */
    // @Override
    public int order() {
        return 9;
    }

    /**
     * 获取活动组件
     *
     * @param activityComponentEnum activityComponentEnum
     * @return T
     * @since 2025/3/21 18:01
     */
    public static <T extends IComponent> T getActivity(ComponentTypeEnum activityComponentEnum) {
        return getActivity(activityComponentEnum.getActivityId());
    }

    /** 获取活动组件 */
    public static <T extends IComponent> T getActivity(int activityId) {
        return (T) ACTIVITY_MAP.get(activityId);
    }

    /** 根据模板id获取活动组件ID集合 */
    public static List<Integer> getActivityIdList(int functionId) {
        return functionIdActivityIdsMap.getOrDefault(functionId, Collections.emptyList());
    }

    /** 获取功能id对应的活动组件集合 */
    public static List<IComponent> getActivityList(int functionId) {
        return getActivityIdList(functionId).stream()
                .<IComponent>map(ComponentManager::getActivity)
                .filter(Objects::nonNull)
                .toList();
    }

    // ===============================================================================================>

    /** 初始化数据 */
    public void initData(long playerId, int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            activity.initData(playerId, functionId);
        }
    }

    /** 校验功能开放 */
    public boolean isOpen(long playerId, int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            if (!activity.isOpen(playerId, functionId)) {
                return false;
            }
        }

        // 默认校验开
        return true;
    }

    /**
     * 处理重置
     * 注：SEASON-赛季类重置触发点为新赛季开启时
     * @param playerId playerId
     * @param functionId functionId
     * @param resetEnum resetEnum
     * @since 2025/3/17 11:39
     */
    public void handleReset(long playerId, int functionId, ResetEnum resetEnum) {
        for (IComponent activity : getActivityList(functionId)) {
            activity.handleReset(playerId, functionId, resetEnum);
        }
    }

    /** 检查并刷新数据 */
    public void checkRefreshData(long playerId, int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            activity.checkRefreshData(playerId, functionId);
        }
    }

    /** 校验红点 */
    public boolean checkRedPoint(long playerId, int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            if (activity.checkRedPoint(playerId, functionId)) {
                return true;
            }
        }
        return false;
    }

    /** 赛季结算（赛季结束触发） */
    public void handlePeriodSettle(long playerId, int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            activity.handlePeriodSettle(playerId, functionId);
        }
    }

    /**
     * 是否有重置
     * @param functionId functionId
     * @return boolean
     * @since 2025/7/18 18:22
     */
    public boolean hasReset(int functionId) {
        for (IComponent activity : getActivityList(functionId)) {
            if (ModifierUtil.hasMethod(activity, "handleReset", long.class, int.class, ResetEnum.class)) {
                return true;
            }
        }
        return false;
    }
}
