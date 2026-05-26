package com.mumu.game.core.drop.consts;

import cn.hutool.core.collection.CollUtil;
import com.mumu.game.business.player.domain.Player;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.drop.manager.RewardConfManager;
import com.mumu.game.proto.item.ItemBean;
import lombok.Getter;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * ItemId
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/24 22:52
 */
public enum ItemId {
    /** 金币 */
    GOLD(1000),
    /** 钻石 */
    DIAM(2000),

    ;

    @Getter
    private final int itemId;

    ItemId(int itemId) {
        this.itemId = itemId;
    }

    /**
     * 获取指定道具掉落数量
     *
     * @param dropStr dropStr
     * @return long
     */
    public long getNum(String dropStr) {
        return Drop.of(dropStr).getDropNum(itemId);
    }

    /** 掉落串中是否有指定道具 */
    public boolean hasDropItem(String dropStr) {
        return Drop.of(dropStr).buildItemBean0(r -> r.getId()== this.getItemId()) != null;
    }

    /** 获取玩家当前道具的数量 */
    public long getOwnNum(long playerId) {
        return getOwnNum(playerId, itemId);
    }

    /** 获取玩家当前道具的数量 */
    public static long getOwnNum(long playerId, int itemId) {
        DropItem reward = RewardConfManager.createSingleReward(itemId);
        return reward == null ? 0 : reward.getOwnNum(playerId);
    }

    /** 是否有此道具 */
    public boolean hasItem(long playerId) {
        return getOwnNum(playerId) > 0;
    }

    /** 获取玩家当前道具的数量 */
    public long getOwnNum(Player player) {
        if (player == null) return 0;

        return getOwnNum(player.getPlayerId());
    }

    /**
     * 获取道具过期时间
     *
     * @param playerId playerId
     * @return long
     */
    public long getExpireTime(long playerId) {
        DropItem reward = RewardConfManager.createSingleReward(itemId);
        return reward == null ? -1 : reward.getExpireTime(playerId);
    }

    /**
     * 获取道具过期时间
     *
     * @param player player
     * @return long
     */
    public long getExpireTime(Player player) {
        return getExpireTime(player.getPlayerId());
    }

    /** 构造当前道具奖励串 */
    public String build(long num, Object... addTime) {
        return buildReward(getItemId(), num, addTime);
    }

    // static method ==============================================================================>

    /** 构造奖励字符串 */
    public static String buildReward(int itemId, long num, Object... addTime) {
        StringJoiner rewardJoiner = new StringJoiner(Symbol.COMMA);
        rewardJoiner.add(Integer.toString(itemId));
        rewardJoiner.add(Long.toString(num));
        for (Object obj : addTime) {
            if (obj != null) rewardJoiner.add(obj.toString());
        }
        return rewardJoiner.toString();
    }

    /** 道具列表转为奖励串 */
    public static String toRewards(List<ItemBean> rewardItems) {
        return rewardItems.stream()
                .map(item -> buildReward(item.getId(), item.getNum(), item.getTime()))
                .collect(Collectors.joining(Symbol.SEMICOLON));
    }

    /** 解析道具配置 */
    public static List<ItemBean> toItemBeans(String rewards) {
        return Drop.of(rewards).buildItemBeans();
    }

    /** 获取道具ItemBean */
    public static ItemBean toItemBean(int itemId, long num, Object... addTime) {
        return CollUtil.getFirst(toItemBeans(buildReward(itemId, num, addTime)));
    }
}
