package com.mumu.game.template.func.core.temp;

import com.mumu.game.business.function.dao.PlayerTemplateManager;
import com.mumu.game.business.function.domain.PlayerTemplate;
import com.mumu.game.business.function.luban.FunctionConfLuban;
import com.mumu.game.business.function.luban.dto.FunctionDTO;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.condition.ConditionParser;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.proto.function.OnPushFunctionStateListMessage;
import com.mumu.game.proto.function.SingleFunctionInfoBean;
import com.mumu.game.proto.function.WCClearFuncRedPointMessage;
import com.mumu.game.template.component.ComponentManager;
import com.mumu.game.template.func.dao.PlayerTemplateStateManager;
import com.mumu.game.template.func.domain.PlayerTemplateState;
import com.mumu.game.template.func.core.FunctionId;
import com.mumu.game.template.func.enums.ResetEnum;
import com.mumu.game.template.func.utils.TemplateUtil;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractTemplate
 * 抽象功能模版
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:30
 */
public abstract class AbstractTemplate implements Template, TempHook {
    @Resource protected PlayerTemplateManager playerTemplateDOOprator;
    @Resource protected ComponentManager activityManager;
    @Resource protected PlayerManager playerManager;
    @Resource protected PlayerTemplateStateManager playerTemplateStateManager;

    @Override
    public void initData(long playerId) {
        // TODO初始化活动组件
        // activityManager.initData(playerId, getFunctionId());
    }

    @Override
    public boolean isOpen(long playerId) {
        long now = System.currentTimeMillis();
        // 活动未开始
        long startTime = getStartTime(playerId);
        if (startTime == 0 || (startTime != -1 && now < startTime)) {
            return false;
        }
        // 活动已过期
        long endTime = getEndTime(playerId);
        if (endTime == 0 || (endTime != -1 && now > endTime)) {
            return false;
        }

        // TODO 活动组件功能开放校验
        /*if (!activityManager.isOpen(playerId, getFunctionId())) {
            return false;
        }*/

        // 配置表校验
        FunctionDTO functionDTO = FunctionConfLuban.getConfigFunction(getFunctionId());
        if (functionDTO == null) {
            return false;
        }
        // 功能总开关
        if (functionDTO.isClose()) {
            return false;
        }

        if (!ConditionParser.of(playerId, functionDTO.getCondition()).checkCondition()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean checkRedPoint(long playerId) {
        // TODO
        // return activityManager.checkRedPoint(playerId, getFunctionId());
        return false;
    }


    @Override
    public void handleReset(long playerId, ResetEnum resetEnum) {
        // TODO
        // activityManager.handleReset(playerId, getFunctionId(), resetEnum);
    }

    @Override
    public void checkRefreshData(long playerId) {
        // TODO
        // activityManager.checkRefreshData(playerId, getFunctionId());
    }


    // ============================== 【Template impl】 ==============================

    @Override
    public PlayerTemplateState getState(long playerId) {
        return getState(playerId, getFunctionId());
    }

    @Override
    public List<PlayerTemplateState> getStateList(long playerId) {
        int functionId = getFunctionId();
        List<PlayerTemplateState> result = new ArrayList<>();

        List<Integer> subFuncIdList = new ArrayList<>();
        // 获取当前功能id，下的所有功能id
        TemplateUtil.getAllSubFunctionId(functionId, subFuncIdList);
        for (int subFuncId : subFuncIdList) {
            result.add(getState(playerId, subFuncId));
        }
        return result;
    }

    @Override
    public ResponseResult clearFuncRedPoint(long playerId, int arg0, String arg1) {
        return ResponseResult.success(playerId, new WCClearFuncRedPointMessage());
    }

    @Override
    public void pushFunctionStateMessage(long playerId) {
        // 红点状态推送
        OnPushFunctionStateListMessage pushMsg = new OnPushFunctionStateListMessage();

        int moduleParentFuncId = getFunctionId();
        moduleParentFuncId = moduleParentFuncId <= 0 ? moduleParentFuncId
                : TemplateUtil.getTopModuleParentFunctionId(moduleParentFuncId);
        // 重新load，状态刷新
        List<SingleFunctionInfoBean> moduleFunctionInfoBeanList = FunctionId.loadFuncTemplate(playerId, moduleParentFuncId)
                .buildSingleFunctionInfoBeanList(playerId, false);
        pushMsg.setFunctionId(moduleParentFuncId);
        pushMsg.setSingleFunctionInfoBeanList(moduleFunctionInfoBeanList);

        // TODO 推送
        // MessageSender.pushToPlayer(playerId, Cmd.OnPushFunctionStateList, pushMsg);
    }

    @Override
    public List<SingleFunctionInfoBean> buildSingleFunctionInfoBeanList(long playerId, boolean buildAll) {
        List<SingleFunctionInfoBean> beanList = new ArrayList<>();

        // 遍历当前所有子功能state
        for (PlayerTemplateState state : getStateList(playerId)) {
            // 更新是否拥有红点（子类有红点，则映射父类有红点）
            state.setHasRedPoint(hasRedPoint(playerId, state.getFunctionId()));

            // 2. 构建bean
            SingleFunctionInfoBean bean = state.convert2Bean(buildAll);
            if (bean == null) {
                continue;
            }

            if (buildAll) {
                bean.setConfigFunctionInfoBean(TemplateUtil.buildConfigFunctionInfoBean(state.getFunctionId()));
            }
            beanList.add(bean);
        }

        return beanList;
    }

    /**
     * 检查本功能以及子功能是否有红点（子有红点，父标记true）
     * @param playerId playerId
     * @param functionId 被检查功能id
     * @return boolean
     * @since 2025/7/8 14:58
     */
    private boolean hasRedPoint(long playerId, int functionId) {
        boolean hasRedPoint = false;
        // 当前是功能是否有红点
        if (getState(playerId, functionId).isHasRedPoint()) {
            hasRedPoint = true;

            // 子功能是否有红点
        } else {
            List<Integer> subFunctionIdList = new ArrayList<>();
            TemplateUtil.getAllSubFunctionId(functionId, subFunctionIdList);
            for (int sunFuncId : subFunctionIdList) {
                if (getState(playerId, sunFuncId).isHasRedPoint()) {
                    hasRedPoint = true;
                    break;
                }
            }
        }

        return hasRedPoint;
    }

    /**
     * 获取玩家模版信息
     * @param playerId playerId
     * @return com.game.business.function.domain.PlayerTemplate
     * @since 2025/7/7 13:41
     */
    protected PlayerTemplate getPlayerTemplate(long playerId) {
        return playerTemplateDOOprator.getOrNew(playerId, getFunctionId());
    }

    /**
     * 获取当前状态
     * @param playerId playerId
     * @param functionId functionId
     * @return com.game.template.func.domain.PlayerTemplateState
     * @since 2025/7/7 14:54
     */
    protected PlayerTemplateState getState(long playerId, int functionId) {
        return playerTemplateStateManager.getOrNew(playerId, functionId);
    }
}
