package com.mumu.game.template.func.core.temp;

import java.util.ArrayList;

import com.mumu.game.business.activity.luban.ActivityConfLuban;
import com.mumu.game.business.function.domain.PlayerTemplate;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.drop.consts.ItemId;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.luban.config.activity.Activity;
import com.mumu.game.template.func.utils.TemplateUtil;

/**
 * AbstractPeriodTemplate
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:30
 */
public abstract class AbstractPeriodTemplate extends AbstractTemplate implements TempPeriodHook {
    private volatile int moduleParentFuncId = 0;
    /** 赛季活动 */
    protected volatile Activity configPeriodActivity;

    @Override
    public final void initData(long playerId) {
        super.initData(playerId);

        moduleParentFuncId = TemplateUtil.getModuleParentFuncId(getFunctionId());
        configPeriodActivity = ActivityConfLuban.findCurrActivityByFunId(moduleParentFuncId);

        // 初始化周期性任务
        initPeriodData(playerId);
    }

    @Override
    public boolean checkRedPoint(long playerId) {
        // 周期活动首次红点
        if (getPlayerTemplate(playerId).hasRedPoint()) {
            return true;
        }

        return activityManager.checkRedPoint(playerId, getFunctionId());
    }

    @Override
    public ResponseResult clearFuncRedPoint(long playerId, int arg0, String arg1) {
        PlayerTemplate playerTemplateDO = getPlayerTemplate(playerId);
        // 点击取消周期活动首次红点
        playerTemplateDO.cleanRedPoint();
        playerTemplateDOOprator.update(playerTemplateDO);

        // 刷新上层红点状态
        pushFunctionStateMessage(playerId);

        return super.clearFuncRedPoint(playerId, arg0, arg1);
    }

    @Override
    public long getStartTime(long playerId) {
        // 当前未在活动期间内，判断是否存配置下一期活动，如果存在，返回下一期活动开始时间
        if (configPeriodActivity == null) {
            Activity nextConfigActivity = ActivityConfLuban.getNextConfigActivityByFunctionId(moduleParentFuncId);
            return nextConfigActivity == null ? 0 : nextConfigActivity.startTime;
        }
        return configPeriodActivity.startTime;
    }

    @Override
    public long getEndTime(long playerId) {
        // 当前未在活动期间内，判断是否存配置下一期活动，如果存在，返回下一期活动结束时间
        if (configPeriodActivity == null) {
            Activity nextConfigActivity = ActivityConfLuban.getNextConfigActivityByFunctionId(
                    moduleParentFuncId);
            return nextConfigActivity == null ? 0 : nextConfigActivity.endTime;
        }
        return configPeriodActivity.endTime;
    }

    @Override
    public void handlePeriodSettle(long playerId) {
        // 清理道具
        clearAllItems(playerId);

        activityManager.handlePeriodSettle(playerId, getFunctionId());
    }

    /**
     * 清除所有道具
     *
     * @since 2025/3/6 16:50
     */
    private void clearAllItems(long playerId) {
        PlayerTemplate playerTemplateDO = getPlayerTemplate(playerId);
        int activityId = playerTemplateDO.getActivityId();
        Activity oldConfigPeriodActivity = ActivityConfLuban.getConfigActivity(activityId);
        if (oldConfigPeriodActivity == null) {
            return;
        }

        ArrayList<Integer> itemIdList = oldConfigPeriodActivity.clearItemIds;
        StringBuilder sb = new StringBuilder();
        for (int itemId : itemIdList) {
            long ownNum = ItemId.getOwnNum(playerId, itemId);
            if (ownNum > 0) {
                sb.append(ItemId.buildReward(itemId, ownNum)).append(Symbol.SEMICOLON);
            }
            
        }

        if (!sb.isEmpty()) {
            Drop.of(sb.toString()).deductItem(PlayerManager.self().getPlayer(playerId),
                DropAction.SEASON_OVER_CLEAR_ITEMS, DropParams.build().setFunctionId(moduleParentFuncId));
        }
    }

    /**
     *
     * @param playerId playerId
     * @since 2025/7/7 14:24
     */
    protected void initPeriodData(long playerId) {}
}
