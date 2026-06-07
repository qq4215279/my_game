package com.mumu.game.template.func.core.temp;

/**
 * TempPeriodHook
 * 周期功能模版钩子
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:26
 */
public interface TempPeriodHook extends TempHook {
    /**
     * 赛季结算（赛季结束触发）
     * @param playerId playerId
     * @since 2025/7/4 17:59
     */
    void handlePeriodSettle(long playerId);
}
