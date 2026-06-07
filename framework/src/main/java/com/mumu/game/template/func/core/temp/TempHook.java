package com.mumu.game.template.func.core.temp;

import com.mumu.game.template.func.enums.ResetEnum;

/**
 * TempHook
 * 功能模版钩子
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:29
 */
public interface TempHook {
    /**
     * 获取功能id
     * @return int
     * @since 2025/7/7 11:25
     */
    int getFunctionId();

    /**
     * 子类初始化数据
     * @param playerId playerId
     * @since 2025/7/4 17:54
     */
    void initData(long playerId);

    /**
     * 是否开启
     *
     * @return boolean
     * @since 2024/11/19 19:35
     */
    boolean isOpen(long playerId);

    /**
     * 检查本功能是否有红点（赋值 FuncTemplateState.hasRedPoint ， 用于服务 hasRedPoint() ）
     * @param playerId playerId
     * @return boolean
     * @since 2025/7/4 17:54
     */
    boolean checkRedPoint(long playerId);

    /**
     * 处理重置
     * @param playerId playerId
     * @param resetEnum resetEnum
     * @since 2025/7/4 17:54
     */
    void handleReset(long playerId, ResetEnum resetEnum);

    /**
     * 检查刷新数据
     * @param playerId playerId
     * @since 2025/7/4 17:54
     */
    void checkRefreshData(long playerId);

    /**
     * 获取赛季开始时间，无默认-1
     * @param playerId playerId
     * @return long
     * @since 2025/7/4 17:55
     */
    default long getStartTime(long playerId) {
        return -1;
    }

    /**
     * 获取赛季结束时间，无默认-1
     * @param playerId playerId
     * @return long
     * @since 2025/7/4 17:55
     */
    default long getEndTime(long playerId) {
        return -1;
    }

    /**
     * 获取客户端返回参数
     * @param playerId playerId
     * @return java.lang.String
     * @since 2025/7/4 17:55
     */
    default String getClientParam(long playerId) {
        return null;
    }
}
