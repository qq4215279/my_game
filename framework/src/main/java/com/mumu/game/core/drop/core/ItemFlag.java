package com.mumu.game.core.drop.core;

import com.mumu.game.business.item.BaseItem;

/**
 * ItemFlag
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:08
 */
public enum ItemFlag {
    /** 计数道具 */
    COUNT,
    /** 限时道具 */
    TIME,
    ;

    /** 判断指定道具是否为当前类型 TODO */
    public boolean isCurr(int itemId) {
        // BaseItem baseItem = ItemConfigManager.getBaseItem(itemId);
        BaseItem baseItem = null;
        return baseItem != null && baseItem.getItemFlag() == this;
    }
}
