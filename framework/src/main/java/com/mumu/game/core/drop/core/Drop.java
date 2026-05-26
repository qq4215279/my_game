package com.mumu.game.core.drop.core;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.core.drop.consts.ItemId;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.drop.item.DropItemResult;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;

/**
 * Drop
 * 掉落
 * @author liuzhen
 * @version 1.0.0 2026/5/24 22:53
 */
public interface Drop {
    /** 构造Drop对象 */
    static Drop of(String dropStr) {
        return new DropImpl(dropStr);
    }

    /** 构造空掉落 */
    static Drop empty() {
        return of("");
    }

    /** 构造金币的Drop对象 */
    static Drop ofGold(long num) {
        return of(ItemId.GOLD.build(num));
    }

    /** 构造钻石的Drop对象 */
    static Drop ofDiam(long num) {
        return of(ItemId.DIAM.build(num));
    }


    // --------------------------------- 奖励串的基础信息 --------------------------
    /** 获取奖励表达式 */
    String getRewardStr();

    /** 获取掉落列表 */
    List<DropItem> getRewardList();

    /** 获取指定道具掉落数量 */
    long getDropNum(int itemId);

    // --------------------------------- 构建道具ItemBean --------------------------
    /** 获取玩家当前道具数据 */
    default Map<Integer, ItemBean> getOwnItems(long playerId) {
        return getOwnItems(playerId, r -> true);
    }

    /** 获取玩家当前道具数据（可过滤出指定道具） */
    Map<Integer, ItemBean> getOwnItems(long playerId, Predicate<DropItem> filter);

    /** 构建掉落信息集 */
    default List<ItemBean> buildItemBeans() {
        return buildItemBeans(r -> true);
    }

    /** 构建掉落信息集（可过滤出指定道具） */
    List<ItemBean> buildItemBeans(Predicate<DropItem> filter);

    /** 构建首个掉落信息 */
    default ItemBean buildItemBean0() {
        return buildItemBean0(r -> true);
    }

    /** 构建首个掉落信息（可过滤出指定道具） */
    ItemBean buildItemBean0(Predicate<DropItem> filter);

    // --------------------------------- 道具操作 --------------------------
    /** 合并掉落 */
    default Drop addDrop(String rewards) {
        return addDrop(of(rewards));
    }

    /** 合并掉落 */
    Drop addDrop(Drop drop);

    /** 道具合并 */
    Drop mergeDrop();

    /** 道具翻倍 */
    Drop multi(int multi);

    // --------------------------------- 道具判断 --------------------------
    /** 道具是否充足 */
    default boolean hasItem(long playerId) {
        return hasItem(playerId, false, DropAction.DROP);
    }

    /** 道具是否充足 pushPopGoods-true道具不足时推送弹出礼包 */
    boolean hasItem(long playerId, boolean pushPopGoods, DropAction dropAction);

    // --------------------------------- 道具掉落 --------------------------
    /**
     * 发放奖励
     * <li>Master（大厅）掉落道具时，直接掉落
     * <li>Slave（游戏等其他服）掉落道具时，RPC到 Master掉落，并在异步回调中更新缓存
     *
     * @param playerId 玩家
     * @param action 事件
     * @param dropParams 掉落奖励-扩展参数数据包
     */
    List<ItemBean> rewardItem(long playerId, DropAction action, DropParams dropParams);

    /** 发放奖励 */
    default List<ItemBean> rewardItem(long playerId, DropAction action) {
        return rewardItem(playerId, action, DropParams.EMPTY);
    }


    // --------------------------------- 道具扣除 --------------------------

    /** 【非World服慎用】同步道具扣除（dropParams-设置道具不足是否弹出礼包） */
    DropItemResult deductItem(long playerId, DropAction action, DropParams dropParams);

    /** 【非World服慎用】道具扣除（dropParams-设置道具不足是否弹出礼包） */
    default DropItemResult deductItem(long playerId, DropAction action) {
        return deductItem(playerId, action, DropParams.EMPTY);
    }

    /** 【非World服慎用】道具扣除（dropParams-设置道具不足是否弹出礼包） */
    default DropItemResult deductItem(Player player, DropAction action, DropParams dropParams) {
        return deductItem(player.getPlayerId(), action, dropParams);
    }

    // --------------------------------- 道具使用 --------------------------

    /** 【非World服慎用】同步使用道具 */
    default DropItemResult useItem(long playerId, DropAction action, DropParams dropParams) {
        return DropItemResult.error(ErrorCode.FAIL);
    }

    /** 清除玩家道具 */
    DropItemResult clearItem(long playerId, DropAction action, DropParams dropParams);
}
