package com.mumu.game.core.drop.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.drop.condition.DeductConditionManager;
import com.mumu.game.core.drop.consts.DropMethod;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.drop.item.DropItemResult;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.drop.item.ItemResource;
import com.mumu.game.core.drop.manager.RewardConfManager;
import com.mumu.game.core.drop.utils.ItemUtil;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.core.log.LogAction;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Pair;

/**
 * DropImpl
 * 掉落实现
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:42
 */
public class DropImpl implements Drop {
    /** 奖励信息 */
    protected String dropStr;
    /** 奖励信息 */
    protected List<DropItem> itemList;
    protected Map<DropMethod, List<DropItem>> methodItemsMap;

    /** 变动前的玩家道具数量 */
    private volatile Map<Integer, ItemBean> beforeOwnItems;
    /** 资源变动的map */
    private volatile Map<Integer, ItemResource> resourceMap = Collections.emptyMap();

    public DropImpl(String dropStr) {
        this.dropStr = dropStr;
        List<DropItem> itemList = RewardConfManager.createRewards(dropStr);
        this.methodItemsMap = parseItems2Map(itemList);
    }

    private Map<DropMethod, List<DropItem>> parseItems2Map(List<DropItem> itemList) {
        return itemList.stream().collect(Collectors.groupingBy(DropItem::getDropMethod));
    }

    // --------------------------------- 奖励串的基础信息 --------------------------
    @Override
    public String getRewardStr() {
        return dropStr;
    }

    @Override
    public List<DropItem> getRewardList() {
        List<DropItem> itemList = new ArrayList<>();
        for (List<DropItem> list : this.methodItemsMap.values()) {
            itemList.addAll(list);
        }
        return itemList;
    }

    /**
     * 获取掉落列表
     * @param dropMethod 掉落方法
     * @return java.util.List<com.mumu.game.core.drop.item.DropItem>
     */
    protected List<DropItem> getDropListByMethod(DropMethod dropMethod) {
        return methodItemsMap.computeIfAbsent(dropMethod, k -> new ArrayList<>(2));
    }

    @Override
    public long getDropNum(int itemId) {
        return itemList.stream().filter(reward -> reward.getId() == itemId).mapToLong(DropItem::getNum).sum();
    }

    /** 获取奖励Map */
    protected Map<Integer, Long> getRewardMap() {
        return itemList.stream().collect(Collectors.toMap(DropItem::getId, DropItem::getNum, Long::sum));
    }

    // --------------------------------- 构建道具ItemBean --------------------------

    @Override
    public Map<Integer, ItemBean> getOwnItems(long playerId, Predicate<DropItem> filter) {
        Map<Integer, ItemBean> map = new HashMap<>(this.itemList.size());
        for (DropItem reward : this.itemList) {
            if (filter.test(reward) && !map.containsKey(reward.getId())) {
                map.put(reward.getId(), reward.getOwnItem(playerId));
            }
        }
        return map;
    }

    @Override
    public List<ItemBean> buildItemBeans(Predicate<DropItem> filter) {
        return this.itemList.stream().filter(filter).map(DropItem::toItemBean).collect(Collectors.toList());
    }

    @Override
    public ItemBean buildItemBean0(Predicate<DropItem> filter) {
        return CollectionUtil.getFirst(buildItemBeans(filter));
    }

    // --------------------------------- 道具操作 --------------------------

    @Override
    public Drop addDrop(Drop drop) {
        if (this.dropStr.endsWith(Symbol.SEMICOLON)) {
            this.dropStr += drop.getRewardStr();
        } else {
            this.dropStr += Symbol.SEMICOLON + drop.getRewardStr();
        }
        this.itemList.addAll(drop.getRewardList());

        drop.getRewardList().forEach(dropItem -> getDropListByMethod(dropItem.getDropMethod()).add(dropItem));

        return this;
    }

    @Override
    public Drop mergeDrop() {
        List<DropItem> reduceList = Lists.newArrayList();
        this.itemList.stream().collect(Collectors.groupingBy(DropItem::getId)).forEach((id, list) -> {
            if (DropType.isMerge(id)) {
                list.stream().reduce(DropItem::merge).ifPresent(reduceList::add);
            } else {
                reduceList.addAll(list);
            }
        });
        this.itemList = reduceList;
        this.methodItemsMap = parseItems2Map(reduceList);

        // 合并暂时不调用
        // refreshDropStr();
        return this;
    }

    @Override
    public Drop multi(int multi) {
        this.itemList.forEach(reward -> reward.multi(multi));
        for (Map.Entry<DropMethod, List<DropItem>> entry : this.methodItemsMap.entrySet()) {
            for (DropItem item : entry.getValue()) {
                item.multi(multi);
            }
        }
        refreshDropStr();
        return this;
    }

    /** 刷新掉落字符串（在翻倍、合并等操作时调用） */
    private void refreshDropStr() {
        this.dropStr = itemList.stream().map(DropItem::toReward).collect(Collectors.joining(Symbol.SEMICOLON));
    }

    @Override
    public boolean hasItem(long playerId, boolean pushPopGoods, DropAction dropAction) {
        return DeductConditionManager.checkDeductItem(playerId, pushPopGoods, this, dropAction)
            .getKey() == ErrorCode.SUCCESS;
    }

    // --------------------------------- 道具掉落、扣除、使用 --------------------------

    @Override
    public List<ItemBean> rewardItem(long playerId, DropAction action, DropParams dropParams) {
        if (CollectionUtil.isEmpty(itemList)) {
            LogTopic.ACTION.info("rewardItem", LogAction.ITEM_CHANGE, "rewardItem is null", "playerId", playerId,
                "action", action, "reward", dropStr, "dropParams", dropParams);
            return Collections.emptyList();
        }

        // 获取掉落前的道具
        beforeOwnItems = getOwnItems(playerId);


        List<DropItem> dropItems = new ArrayList<>();
        for (Map.Entry<DropMethod, List<DropItem>> entry : this.methodItemsMap.entrySet()) {
            DropMethod dropMethod = entry.getKey();
            List<DropItem> finalDropList = dropMethod.getFinalDropList(playerId, entry.getValue());
            dropItems.addAll(finalDropList);
        }

        List<ItemBean> result = Lists.newArrayList();
        for (DropItem dropItem : dropItems) {
            if (!dropItem.drop(playerId, action)) {
                LogTopic.ACTION.error("rewardItem", LogAction.ITEM_CHANGE, "rewardItem error", "action", action,
                    "playerId", playerId, "dropItem", dropItem, "dropParams", dropParams);
                continue;
            }
            result.add(dropItem.toItemBean());
        }
        LogTopic.ACTION.info(playerId, "rewardItem", LogAction.ITEM_CHANGE, "action", action, "reward", dropStr,
            "dropParams", dropParams);

        // 设置资源变动map
        resourceMap = ItemUtil.buildItemResourceMap(beforeOwnItems, getOwnItems(playerId));
        // 发布奖励获得事件
        // ActionUtil.rewardItemTrigger(playerId, action, getRewardMap(), dropParams.getExtMap(), resourceMap.values());
        return result;
    }

    /** 扣除道具 pushPopGoods-true道具不足时推送弹出礼包 */
    @Override
    public DropItemResult deductItem(long playerId, DropAction action, DropParams dropParams) {
        // 强制扣除时，不检测所有道具数量是否足够
        if (!dropParams.isForceDeduct()) {
            Pair<ErrorCode, DropItemResult.PopGoods> pair =
                DeductConditionManager.checkDeductItem(playerId, dropParams.isNeedPopGoods(), this, action);
            if (pair.getKey() != ErrorCode.SUCCESS) {
                return DropItemResult.error(pair.getKey(), pair.getValue());
            }
        }
        // 获取掉落前的道具
        beforeOwnItems = getOwnItems(playerId);

        for (DropItem dropItem : itemList) {
            if (!dropItem.deduct(playerId)) {
                LogTopic.ACTION.error("deductItem", LogAction.ITEM_CHANGE, "action", action, "playerId", playerId,
                    "dropItem", dropStr, "deductId", dropItem.getId(), "deductNum", dropItem.getNum(), "dropParams",
                    dropParams);
                return DropItemResult.error(ErrorCode.FAIL_ITEM_NOT_ENOUGH);
            }
        }
        LogTopic.ACTION.info(playerId, "deductItem", LogAction.ITEM_CHANGE, "action", action, "reward", dropStr,
            "dropParams", dropParams);

        // 设置资源变动map
        resourceMap = ItemUtil.buildItemResourceMap(beforeOwnItems, getOwnItems(playerId));
        // 发布道具扣除事件
        // ActionUtil.consumeItemTrigger(playerId, action, getRewardMap(), dropParams.getExtMap(),
        // resourceMap.values());
        return DropItemResult.success(resourceMap);
    }

    @Override
    public DropItemResult useItem(long playerId, DropAction action, DropParams dropParams) {
        Pair<ErrorCode, DropItemResult.PopGoods> pair =
            DeductConditionManager.checkDeductItem(playerId, dropParams.isNeedPopGoods(), this, action);
        if (pair.getKey() != ErrorCode.SUCCESS) {
            return DropItemResult.error(pair.getKey(), pair.getValue());
        }

        // 获取使用前的道具
        beforeOwnItems = getOwnItems(playerId);

        // 开始扣减
        List<ItemBean> useRewardList = Lists.newArrayList();
        for (DropItem reward : itemList) {
            DropItemResult useResult = reward.use(playerId, action);
            if (useResult.isError()) {
                LogTopic.ACTION.error("useItem", LogAction.ITEM_CHANGE, "action", action, "playerId", playerId,
                    "reward", dropStr, "useId", reward.getId(), "useNum", reward.getNum(), "errorCode",
                    useResult.getCode(), "dropParams", dropParams);
                return useResult;
            }
            useRewardList.addAll(useResult.getRewardItems());
        }

        LogTopic.ACTION.info(playerId, "useItem", LogAction.ITEM_CHANGE, "action", action, "reward", dropStr,
            "dropParams", dropParams, "useRewardList", useRewardList);

        // 设置资源变动map
        resourceMap = ItemUtil.buildItemResourceMap(beforeOwnItems, getOwnItems(playerId));

        // 发布道具扣除事件
        // ActionUtil.consumeItemTrigger(playerId, action, getRewardMap(), dropParams.getExtMap(),
        // resourceMap.values());

        return DropItemResult.success(resourceMap, useRewardList);
    }

    @Override
    public DropItemResult clearItem(long playerId, DropAction action, DropParams dropParams) {
        if (CollectionUtil.isEmpty(itemList)) {
            LogTopic.ACTION.info("clearItem", LogAction.ITEM_CHANGE, "clearItem is null", "playerId", playerId,
                "action", action, "reward", dropStr, "dropParams", dropParams);
            return DropItemResult.error(ErrorCode.FAIL_DROP_EMPTY);
        }

        // 获取掉落前的道具
        beforeOwnItems = getOwnItems(playerId);

        List<ItemBean> result = Lists.newArrayList();
        for (DropItem reward : itemList) {
            ItemBean clear = reward.clear(playerId);
            if (clear.getNum() == 0) {
                LogTopic.ACTION.error("clearItem", LogAction.ITEM_CHANGE, "clearItem error", "action", action,
                    "playerId", playerId, "reward", reward, "dropParams", dropParams);
                continue;
            }
            result.add(clear);
        }

        LogTopic.ACTION.info(playerId, "clearItem", LogAction.ITEM_CHANGE, "action", action, "reward", dropStr,
            "dropParams", dropParams);

        // 被清除道具信息
        Map<Integer, Long> clearMap = result.stream().collect(Collectors.toMap(ItemBean::getId, ItemBean::getNum));

        // 设置资源变动map
        resourceMap = ItemUtil.buildItemResourceMap(beforeOwnItems, getOwnItems(playerId));
        // 发布奖励获得事件
        // ActionUtil.rewardItemTrigger(playerId, action, clearMap, dropParams.getExtMap(), resourceMap.values());

        return DropItemResult.success(resourceMap);
    }
}
