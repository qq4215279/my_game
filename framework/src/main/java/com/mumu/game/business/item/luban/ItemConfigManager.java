package com.mumu.game.business.item.luban;

import com.mumu.game.business.item.BaseItem;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.log.LogTopic;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ItemConfigManager
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:47
 */
@Component
public class ItemConfigManager {
    /** 基础道具 key-itemId */
    private static volatile Map<Integer, BaseItem> baseItemMap = Collections.emptyMap();

    /** 道具掉落类型id集 */
    private static volatile Map<Integer, Set<Integer>> rewardTypeIdMap = Collections.emptyMap();
    public static DropType getRewardType(int id) {
        return null;
    }


    /** 获取道具配置 */
    public static BaseItem getBaseItem(int itemId) {
        BaseItem baseItem = baseItemMap.get(itemId);
        if (baseItem == null) {
            LogTopic.ACTION.warn("ItemConfigManager 道具不存在", "itemId", itemId, LogTopic.getStackTrace());
        }
        return baseItem;
    }

    /**
     * getBaseItemList
     *
     * @return java.util.Collection<com.game.business.item.data.BaseItem>
     * @since 2024/11/19 11:15
     */
    public static Collection<BaseItem> getBaseItemList() {
        return baseItemMap.values();
    }

    /** 获取奖励类型的全部道具 */
    public static Set<Integer> getRewardTypeIds(int type) {
        return rewardTypeIdMap.getOrDefault(type, Collections.emptySet());
    }

    /** 是否在背包显示 */
    public static boolean showInPack(int itemId) {
        return Optional.ofNullable(getBaseItem(itemId)).map(BaseItem::isShowPack).orElse(false);
    }

}
