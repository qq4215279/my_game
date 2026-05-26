package com.mumu.game.core.drop.item.impl;

import com.mumu.game.core.drop.anno.DropMapping;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.proto.item.ItemBean;

/**
 * DropItemDiam
 * 钻石掉落
 * @author liuzhen
 * @version 1.0.0 2026/5/25 21:16
 */
@DropMapping(DropType.RewardDiam)
public class DropItemDiam extends DropItem {

    /**
     * 通过道具ID构造奖励类型（用于获取）
     *
     * @param id
     */
    public DropItemDiam(int id) {
        super(id);
    }

    @Override
    public boolean drop(long playerId, DropAction action) {
        return false;
    }

    @Override
    public boolean deduct(long playerId) {
        return false;
    }

    @Override
    public ItemBean clear(long playerId) {
        return null;
    }

    @Override
    public long getOwnNum(long playerId) {
        return 0;
    }
}
