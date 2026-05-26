package com.mumu.game.core.drop.consts;

import com.mumu.game.business.item.luban.ItemConfigManager;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.proto.item.ItemBean;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DropType
 * 掉落类型
 * @author liuzhen
 * @version 1.0.0 2026/5/24 22:29
 */
@Getter
public enum DropType {
    /** NONE */
    NONE(0),
    /** 金币 */
    RewardGold(1),
    /** 钻石 */
    RewardDiam(2),


    ;

    /** 道具类型 */
    @Getter
    private final int type;

    /** 道具是否需要合并 */
    @Getter
    private boolean merge = true;


    DropType(int type) {
        this.type = type;
    }

    /** 是否包含此道具 */
    public boolean contains(int itemId) {
        return ItemConfigManager.getRewardTypeIds(this.getType()).contains(itemId);
    }

    /** 从奖励掉落对象中获取一个当前类型的道具ID，可能为null */
    public Integer findItemId(Drop drop) {
        return Optional.ofNullable(drop.buildItemBean0(r -> r.getType() == this))
                .map(ItemBean::getId)
                .orElse(null);
    }


    /** 判断指定道具能否合并 */
    public static boolean isMerge(int itemId) {
        return Optional.ofNullable(ItemConfigManager.getRewardType(itemId))
                .map(DropType::isMerge)
                .orElse(false);
    }

    /** 道具类型Map key-type value-类型枚举 */
    private static final Map<Integer, DropType> MAP =
            Arrays.stream(values())
                    .filter(o -> o != NONE)
                    .collect(Collectors.toMap(DropType::getType, t -> t));
}
