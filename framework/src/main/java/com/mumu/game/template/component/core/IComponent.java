package com.mumu.game.template.component.core;

import com.mumu.game.proto.component.WCActivityInfoMessage;
import com.mumu.game.template.func.enums.ResetEnum;

/**
 * IComponent
 * 组件接口
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:22
 */
public interface IComponent {

    /**
     * 初始化数据
     * @param playerId playerId
     * @param functionId functionId
     * @since 2025/3/15 20:59
     */
    void initData(long playerId, int functionId);

    /**
     * 功能是否开启
     * @return boolean
     * @since 2025/3/16 14:20
     */
    boolean isOpen(long playerId, int functionId);

    /**
     * 重置
     * 注：SEASON-赛季类重置触发点为新赛季开启时
     * @param playerId playerId
     * @param functionId functionId
     * @param resetEnum resetEnum
     * @since 2025/3/15 21:15
     */
    void handleReset(long playerId, int functionId, ResetEnum resetEnum);

    /** 检查并刷新数据 */
    void checkRefreshData(long playerId, int functionId);

    /**
     * 校验红点
     * @param playerId playerId
     * @param functionId functionId
     * @return boolean
     * @since 2025/3/15 20:59
     */
    boolean checkRedPoint(long playerId, int functionId);

    /**
     * 处理周期类活动重置（赛季结束触发）
     * @param playerId playerId
     * @param functionId functionId
     * @since 2025/6/4 11:50
     */
    void handlePeriodSettle(long playerId, int functionId);

    /** 填充活动面板信息 */
    void fillActivityInfo(long playerId, int functionId, String param, WCActivityInfoMessage resMsg);
}
