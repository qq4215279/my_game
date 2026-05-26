package com.mumu.game.core.drop.item.impl;

import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.proto.item.ItemBean;

/**
 * DropItemPack
 * 背包掉落
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:14
 */
public class DropItemPack extends DropItem {

    public DropItemPack(int id) {
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
