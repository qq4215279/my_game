package com.mumu.game.core.drop.utils;

import com.google.common.collect.Lists;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.drop.core.ItemFlag;
import com.mumu.game.core.drop.item.ItemResource;
import com.mumu.game.proto.item.ItemBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ItemUtil
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/25 21:07
 */
public class ItemUtil {
    /** 获取资源变动Map k-itemId，val-最新的资源信息 */
    public static Map<Integer, ItemResource> buildItemResourceMap(
            Map<Integer, ItemBean> beforeOwnItems, Map<Integer, ItemBean> afterOwnItems) {
        Map<Integer, ItemResource> resoureMap = new HashMap<>();

        for (ItemBean before : beforeOwnItems.values()) {
            ItemBean after = afterOwnItems.get(before.getId());
            long change = after.getNum() - before.getNum();
            resoureMap.put(
                    before.getId(),
                    ItemResource.of(before.getId(), after.getNum(), after.getExpireTime(), change, after.getHasPoint()));
        }
        return resoureMap;
    }

    /**
     * 合并奖励道具
     * @param tmpRewards tmpRewards
     * @return java.util.List<com.game.proto.item.ItemBean>
     * @since 2025/1/22 13:53
     */
    public static List<ItemBean> mergeRewards(List<ItemBean> tmpRewards) {
        List<ItemBean> reduceList = Lists.newArrayList();
        tmpRewards.stream()
                .collect(Collectors.groupingBy(ItemBean::getId))
                .forEach(
                        (id, list) -> {
                            if (DropType.isMerge(id)) {
                                list.stream().reduce((o1, o2) -> {

                                    if (ItemFlag.COUNT.isCurr(id)) {
                                        // 计数道具
                                        o1.setNum(o1.getNum() + o2.getNum());
                                    } else {
                                        // 计时道具
                                        if (o1.getNum() > 1) {
                                            o1.setTime(o1.getTime() * o1.getNum());
                                            o1.setNum(1L);
                                        }

                                        if (o1.getTime() == -1 || o2.getTime() == -1)
                                            o1.setTime((long) -1);
                                        else o1.setTime(o1.getTime() + o2.getTime() * o2.getNum());
                                    }
                                    return o1;

                                }).ifPresent(reduceList::add);
                            } else {
                                reduceList.addAll(list);
                            }
                        });

        return reduceList;
    }
}
