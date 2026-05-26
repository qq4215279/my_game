package com.mumu.game.core.drop.condition;

import com.mumu.game.proto.item.PopGoodsReason;

/**
 * DeductCondition
 * 扣除条件 扣除 Condition
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:54
 */
public interface DeductCondition {
    /**
     * 弹窗原因
     * @return com.game.proto.shop.PopGoodsReason
     */
    PopGoodsReason getPopGoodsReason();

    /**
     * 获取冻结金币数量
     *
     * @param playerId playerId
     * @param itemId itemId
     * @param ownNum ownNum
     * @param costCount costCount
     * @return int
     */
    long getFreezeItemNum(long playerId, int itemId, long ownNum, long costCount);
}
