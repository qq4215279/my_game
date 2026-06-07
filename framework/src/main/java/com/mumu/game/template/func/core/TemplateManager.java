package com.mumu.game.template.func.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mumu.game.business.activity.luban.ActivityConfLuban;
import com.mumu.game.business.function.dao.PlayerTemplateManager;
import com.mumu.game.business.function.domain.PlayerTemplate;
import com.mumu.game.business.function.luban.FunctionConfLuban;
import com.mumu.game.business.function.luban.dto.FunctionDTO;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.utils.ModifierUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.luban.config.activity.Activity;
import com.mumu.game.template.component.ComponentManager;
import com.mumu.game.template.func.core.temp.DefaultPeriodTemplate;
import com.mumu.game.template.func.core.temp.DefaultTemplate;
import com.mumu.game.template.func.core.temp.TempHook;
import com.mumu.game.template.func.core.temp.TempPeriodHook;
import com.mumu.game.template.func.core.temp.Template;
import com.mumu.game.template.func.dao.PlayerTemplateStateManager;
import com.mumu.game.template.func.domain.PlayerTemplateState;
import com.mumu.game.template.func.enums.ResetEnum;
import com.mumu.game.template.func.utils.TemplateUtil;

import cn.hutool.core.lang.Assert;

/**
 * TemplateManager
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 14:32
 */
@Component
public class TemplateManager {
    /** 结算状态 */
    private static final short CACL_STATE = 1;
    private static final LogTopic log = LogTopic.ACTION;

    private static PlayerTemplateManager playerTemplateDOOprator;
    private static PlayerTemplateStateManager playerTemplateStateManager;
    private static ComponentManager activityManager;

    @Autowired
    @SuppressWarnings("all")
    public void setPlayerTemplateOperator(PlayerTemplateManager playerTemplateDOOprator) {
        TemplateManager.playerTemplateDOOprator = playerTemplateDOOprator;
    }

    @Autowired
    public void setPlayerTemplateStateManager(PlayerTemplateStateManager playerTemplateStateManager) {
        TemplateManager.playerTemplateStateManager = playerTemplateStateManager;
    }

    @Autowired
    public void setActivityManager(ComponentManager activityManager) {
        TemplateManager.activityManager = activityManager;
    }


    /** 功能id 与 功能 映射 */
    private static Map<Integer, Template> TEMPLATE_MAP = Maps.newHashMap();
    /** 功能 与 功能id 映射 */
    private static Map<Template, Integer> CLAZZNAME_TEMPLATE_MAP = Maps.newHashMap();
    /** 功能id 与 服务组列表 映射 */
    private static Map<Integer, ServiceType> FUNCTIONID_SERVER_GROUPS_MAP = Maps.newHashMap();
    /** 服务器组 与 功能id列表 映射 */
    private static Map<ServiceType, List<Integer>> SERVER_GROUP_FUNCTIONIDS_MAP = Maps.newHashMap();
    /** 功能id 是否有重置逻辑 */
    private static Map<Integer, Boolean> FUNC_ID_NO_RESET = Maps.newHashMap();


    // @Override
    @SuppressWarnings("all")
    public void autoInit() {
        Map<Integer, Template> TMP_TEMPLATE_MAP = Maps.newHashMap();
        Map<Template, Integer> TMP_CLAZZNAME_TEMPLATE_MAP = Maps.newHashMap();
        Map<Integer, ServiceType> TMP_FUNCTIONID_SERVER_GROUPS_MAP = Maps.newHashMap();
        Map<ServiceType, List<Integer>> TMP_SERVER_GROUP_FUNCTIONIDS_MAP = Maps.newHashMap();

        Map<Integer, Boolean> TMP_FUNC_ID_NO_RESET = Maps.newHashMap();

        Map<String, Template> templateMap = SpringContextUtils.getBeansOfType(Template.class);
        for (Template template : templateMap.values()) {
            int functionId = template.getFunctionId();
            if (functionId == 0) {
                continue;
            }
            ServiceType loadServerGroup = findServerGroup(functionId);
            // 是当前服加载
            if (ServiceType.curr() == loadServerGroup) {
                TMP_TEMPLATE_MAP.put(functionId, template);
                TMP_CLAZZNAME_TEMPLATE_MAP.put(template, functionId);
                TMP_FUNCTIONID_SERVER_GROUPS_MAP.put(functionId, loadServerGroup);
                TMP_SERVER_GROUP_FUNCTIONIDS_MAP.computeIfAbsent(loadServerGroup, k -> new ArrayList<>()).add(functionId);

                TMP_FUNC_ID_NO_RESET.put(functionId, ModifierUtil.hasMethod(template, "handleReset", long.class, ResetEnum.class));
            }

        }

        TEMPLATE_MAP = TMP_TEMPLATE_MAP;
        CLAZZNAME_TEMPLATE_MAP = TMP_CLAZZNAME_TEMPLATE_MAP;
        FUNCTIONID_SERVER_GROUPS_MAP = TMP_FUNCTIONID_SERVER_GROUPS_MAP;
        SERVER_GROUP_FUNCTIONIDS_MAP = TMP_SERVER_GROUP_FUNCTIONIDS_MAP;
        FUNC_ID_NO_RESET = TMP_FUNC_ID_NO_RESET;

        initFuncTemplateMap();
    }

    // @Override
    public void autoLubanRefresh() {
        initFuncTemplateMap();

        // TODO 全局在线玩家推送配置表变更
        // MessageSender.broadcast(Cmd.OnWCPushConfigFuncUpdate, ErrorCode.SUCCESS, new OnWCPushConfigFuncUpdateMessage());
    }

    /**
     * 刷新功能表配置
     *
     * @since 2024/11/20 13:51
     */
    @SuppressWarnings("all")
    private void initFuncTemplateMap() {
        Map<Integer, Template> TMP_TEMPLATE_MAP = Maps.newHashMap(TEMPLATE_MAP);
        Map<Template, Integer> TMP_CLAZZNAME_TEMPLATE_MAP = Maps.newHashMap(CLAZZNAME_TEMPLATE_MAP);
        Map<Integer, ServiceType> TMP_FUNCTIONID_SERVER_GROUPS_MAP = Maps.newHashMap(FUNCTIONID_SERVER_GROUPS_MAP);
        Map<ServiceType, List<Integer>> TMP_SERVER_GROUP_FUNCTIONIDS_MAP = Maps.newHashMap(SERVER_GROUP_FUNCTIONIDS_MAP);
        Map<Integer, Boolean> TMP_FUNC_ID_NO_RESET = Maps.newHashMap(FUNC_ID_NO_RESET);

        // 初始化
        ImmutableMap<Integer, FunctionDTO> functionIdItemInfoMap = FunctionConfLuban.getFunctionMap();
        for (Map.Entry<Integer, FunctionDTO> entry : functionIdItemInfoMap.entrySet()) {
            int functionId = entry.getKey();
            if (TMP_TEMPLATE_MAP.containsKey(functionId)) {
                // 判定是否有重置逻辑
                if (!TMP_FUNC_ID_NO_RESET.getOrDefault(functionId, false)) {
                    TMP_FUNC_ID_NO_RESET.put(functionId, activityManager.hasReset(functionId));
                }

                continue;
            }

            ServiceType loadServerGroup = findServerGroup(functionId);
            TMP_FUNCTIONID_SERVER_GROUPS_MAP.put(functionId, loadServerGroup);
            TMP_SERVER_GROUP_FUNCTIONIDS_MAP.computeIfAbsent(loadServerGroup, k -> new ArrayList<>()).add(functionId);

            // 是当前服加载
            if (ServiceType.curr() == loadServerGroup) {
                Class<? extends Template> clazz = ActivityConfLuban.isPeriodActivity(functionId)
                        ? DefaultPeriodTemplate.class : DefaultTemplate.class;
                Template template = SpringContextUtils.getBean(clazz);
                if (template instanceof DefaultTemplate abstractTemplate) {
                    abstractTemplate.setFunctionId(functionId);
                } else if (template instanceof DefaultPeriodTemplate defaultPeriodTemplate) {
                    defaultPeriodTemplate.setFunctionId(functionId);
                }

                TMP_TEMPLATE_MAP.put(functionId, template);
                TMP_CLAZZNAME_TEMPLATE_MAP.put(template, functionId);

                TMP_FUNC_ID_NO_RESET.put(functionId, activityManager.hasReset(functionId));
            }

        }

        TEMPLATE_MAP = TMP_TEMPLATE_MAP;
        CLAZZNAME_TEMPLATE_MAP = TMP_CLAZZNAME_TEMPLATE_MAP;
        FUNCTIONID_SERVER_GROUPS_MAP = TMP_FUNCTIONID_SERVER_GROUPS_MAP;
        SERVER_GROUP_FUNCTIONIDS_MAP = TMP_SERVER_GROUP_FUNCTIONIDS_MAP;
        FUNC_ID_NO_RESET = TMP_FUNC_ID_NO_RESET;
    }

    /**
     * 查找指定功能id，所在服务组列表
     *
     * @param functionId functionId
     * @return java.util.List<com.game.framework.net.consts.ServiceType>
     * @since 2025/4/16 15:27
     */
    public static ServiceType findServerGroup(int functionId) {
        // 根节点
        if (functionId == FunctionId.ROOT.getFunctionId()) {
            return ServiceType.WORLD;
        }

        FunctionId functionIdEnum = FunctionId.getFunctionId(functionId);
        if (functionIdEnum == null) {
            return ServiceType.WORLD; // todo FunctionIdEnum中未定义的子功能id，默认为WORLD，但父节点可能在其他服
        }

        /*if (functionIdEnum.getServerGroup() != null) {
            return functionIdEnum.getServerGroup();
        }*/

        FunctionDTO configFunctionDTO = FunctionConfLuban.getConfigFunction(functionId);
        Assert.notNull(configFunctionDTO, "functionId {} is null", functionId);
        int parentId = configFunctionDTO.getParentId();
        return findServerGroup(parentId);
    }

    // @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.COMMON;
    }


    /**
     * 加载功能模版
     * @param playerId playerId
     * @param functionId functionId
     * @param loadRemoteState 是否需要预加载远程模版的PlayerTemplateState
     * @return com.game.template.func.core.template.Template2
     * @since 2025/4/16 11:35
     */
    static Template loadFuncTemplate(long playerId, int functionId, boolean loadRemoteState) {
        return doLoadFuncTemplate(playerId, functionId, loadRemoteState);
    }

    /**
     * 加载功能模版(默认无需加载远程模版state列表)
     * @param playerId playerId
     * @param functionId functionId
     * @return com.game.template.func.core.template.Template2
     * @since 2025/4/16 11:35
     */
    static Template loadFuncTemplate(long playerId, int functionId) {
        return loadFuncTemplate(playerId, functionId, false);
    }

    /**
     * loadFuncTemplate
     * @param playerId 玩家id
     * @param functionId 功能id
     * @param loadRemoteState 是否需要预加载远程模版的PlayerTemplateState
     * @return com.game.template.func.core.template.FuncTemplate
     * @since 2024/11/19 19:32
     */
    private static Template doLoadFuncTemplate(long playerId, int functionId, boolean loadRemoteState) {
        // TODO 远程加载template
       /* if (!checkInServerGroup(functionId)) {
            return ProxyTemplate.of(getServerGroup(functionId),
                    playerId, functionId, loadRemoteState, playerTemplateStateManager);
        }*/

        Template template = null;
        try {
            // 每次获取新的空对象
            template = TEMPLATE_MAP.get(functionId);

            // 初始化
            if (template instanceof TempHook tempHook) {
                // 1. 绑定玩家基本信息
                tempHook.initData(playerId);

                // 2. 检查并初始化
                checkAndInitTemplateData(tempHook, playerId, loadRemoteState);
            }

        } catch (Exception e) {
            log.error(
                    e,
                    "loadFuncTemplate error",
                    "playerId",
                    playerId,
                    "functionId",
                    functionId,
                    "checkSubFuncTemplate",
                    true);
        }
        return template;
    }

    /**
     * 检查功能id，是否隶属于当前服务器
     *
     * @param functionId functionId
     * @return boolean
     * @since 2025/4/15 20:06
     */
    @SuppressWarnings("all")
    public static boolean checkInServerGroup(int functionId) {
        ServiceType curr = ServiceType.curr();
        ServiceType targetServerGroup = getServerGroup(functionId);
        return targetServerGroup != null && curr == targetServerGroup;
    }

    /**
     * 获取功能id所在服务器组
     * @param functionId functionId
     * @return com.game.framework.net.consts.ServiceType
     * @since 2025/8/6 10:29
     */
    public static ServiceType getServerGroup(int functionId) {
        return FUNCTIONID_SERVER_GROUPS_MAP.get(functionId);
    }

    /**
     * 获取功能id列表By功能id所在服务器组
     * @param serverGroup serverGroup
     * @return java.util.List<java.lang.Integer>
     * @since 2025/8/5 17:35
     */
    public static List<Integer> getFuncIdListByServerGroup(ServiceType serverGroup) {
        return new ArrayList<>(SERVER_GROUP_FUNCTIONIDS_MAP.getOrDefault(serverGroup, Collections.emptyList()));
    }

    // ==============================>

    /**
     * 校验并初始化模版数据
     * @param template template
     * @param playerId playerId
     * @param loadRemoteState 是否需要预加载远程模版的PlayerTemplateState
     * @since 2025/7/5 10:16
     */
    private static void checkAndInitTemplateData(TempHook template, long playerId, boolean loadRemoteState) {
        boolean open = template.isOpen(playerId);

        // 1.1. 周期性功能模版 => 检查并刷新周期性数据
        if (template instanceof TempPeriodHook periodTemplate) {
            checkAndRefreshPeriodData(periodTemplate, playerId, open);

            // 1.2. 非周期性功能模版 => 检查并刷新数据
        } else {
            checkAndRefreshData(template, playerId, open);
        }

        // 2. 初始化模版state数据
        updateTemplateState(template, playerId, open);

        // 3. 检查子类模版，并递归初始化子类模版
        if (open) {
            checkSubFuncTemplate(playerId, template.getFunctionId(), loadRemoteState);
        }
    }

    // ==================================== 【非周期性 - api】 ====================================

    /**
     * 检查并刷新数据
     * @param template template
     * @param playerId playerId
     * @param open open
     * @since 2025/7/7 14:51
     */
    private static void checkAndRefreshData(TempHook template, long playerId, boolean open) {
        if (open) {
            // 有重写重置逻辑，则重置数据。 目的：懒加载 PlayerTemplate
            if (FUNC_ID_NO_RESET.getOrDefault(template.getFunctionId(), false)) {
                checkResetData(template, playerId);
            }

            // 刷新数据
            template.checkRefreshData(playerId);
        }
    }

    /**
     * 每日、每周、每月，重置校验
     *
     * @since 2024/12/4 20:06
     */
    private static void checkResetData(TempHook template, long playerId) {
        PlayerTemplate playerTemplateDO = playerTemplateDOOprator.getOrNew(playerId, template.getFunctionId());
        boolean needSave = false;
        for (ResetEnum resetEnum : ResetEnum.values()) {
            if (resetEnum.needReset(playerTemplateDO.getLastResetTime())) {
                template.handleReset(playerId, resetEnum);
                needSave = true;
            }
        }
        // 变更save
        if (needSave) {
            playerTemplateDO.dailyReset(System.currentTimeMillis());
            playerTemplateDOOprator.update(playerTemplateDO);
        }
    }

    /** 更新PlayerTemplateState */
    private static void updateTemplateState(TempHook template, long playerId, boolean open) {
        int functionId = template.getFunctionId();
        PlayerTemplateState templateState = playerTemplateStateManager.getOrNew(playerId, functionId);
        // 功能开放
        templateState.setOpen(open);
        // 红点
        if (open) {
            templateState.setHasRedPoint(template.checkRedPoint(playerId));
        }
        // TODO 赋值 开放的活动模板类型ID
        // templateState.setActivityIds(ActivityManager.getActivityIdList(functionId));
        // 赛季开始时间，无默认-1
        templateState.setStartTime(template.getStartTime(playerId));
        //  赛季结束时间，无默认-1
        templateState.setEndTime(template.getEndTime(playerId));
        // 参数
        templateState.setClientParam(template.getClientParam(playerId));

        // 更新(JVM缓存无需update)
        // playerTemplateStateManager.update(templateState);
    }

    /** 递归检查子功能状态 */
    private static void checkSubFuncTemplate(long playerId, int functionId, boolean loadRemoteState) {
        List<Integer> subFunctionIds = TemplateUtil.getSubFunctionIdList(functionId);
        for (int subId : subFunctionIds) {
            doLoadFuncTemplate(playerId, subId, loadRemoteState);
        }
    }


    // ==================================== 【周期性 - api】 ====================================

    /**
     * 检查并刷新周期性数据
     * @param periodTemplate periodTemplate
     * @param playerId playerId
     * @param open open
     * @since 2025/4/2 10:49
     */
    private static void checkAndRefreshPeriodData(TempPeriodHook periodTemplate, long playerId, boolean open) {
        int functionId = periodTemplate.getFunctionId();
        int moduleParentFuncId = TemplateUtil.getModuleParentFuncId(functionId);
        // 赛季活动
        Activity configPeriodActivity = ActivityConfLuban.findCurrActivityByFunId(moduleParentFuncId);

        PlayerTemplate playerTemplateDO = playerTemplateDOOprator.getOrNull(playerId, functionId);

        long now = System.currentTimeMillis();

        // 不为null => 当前活动正在进行中
        if (configPeriodActivity != null) {
            int newActivityId = configPeriodActivity.id;
            // 当前进行中的活动id与玩家参与的活动ID不同，需要结算
            if (playerTemplateDO == null || playerTemplateDO.getActivityId() != newActivityId) {
                // 1.1. 检查并结算上个活动数据
                if (checkPeriodSettle(playerTemplateDO)) {
                    doPeriodSettle(periodTemplate, playerTemplateDO, now);
                }

                // 1.2. 当前活动开始，重置历史数据，并初始化新周期数据
                if (open) {
                    for (ResetEnum resetEnum : ResetEnum.CAN_RESET_TYPES) {
                        periodTemplate.handleReset(playerId, resetEnum);
                    }

                    // 初始化 playerTemplateDO
                    if (playerTemplateDO == null) {
                        playerTemplateDO = playerTemplateDOOprator.getOrNew(playerId, functionId);
                    }
                    playerTemplateDO.periodReset();
                    playerTemplateDO.setActivityId(newActivityId);

                    // 刷新数据(活动开启 => 初始化新赛季数据)
                    periodTemplate.checkRefreshData(playerId);
                }
                if (playerTemplateDO != null) {
                    playerTemplateDOOprator.update(playerTemplateDO);
                }

                // 进行中的活动
            } else {
                // 进行中的活动，直接调用父类刷新
                checkAndRefreshData(periodTemplate, playerId, open);
            }
            // 2. 没有新周期活动，表示上期已结束，且 未结算过，结束上期数据
        } else if (checkPeriodSettle(playerTemplateDO)) {
            doPeriodSettle(periodTemplate, playerTemplateDO, now);
            playerTemplateDOOprator.update(playerTemplateDO);
        }
    }


    /** 是否进行过赛季结算 */
    private static boolean checkPeriodSettle(PlayerTemplate playerTemplateDO) {
        return playerTemplateDO != null && playerTemplateDO.getActivityId() > 0
                && playerTemplateDO.getSeasonCaclState() != CACL_STATE;
    }

    /** do 赛季结算 */
    private static void doPeriodSettle(TempPeriodHook periodTemplate, PlayerTemplate playerTemplateDO, long now) {
        // 赛季结算
        periodTemplate.handlePeriodSettle(playerTemplateDO.getPlayerId());
        playerTemplateDO.dailyReset(now);
        playerTemplateDO.setSeasonCaclState(CACL_STATE);
    }
}
