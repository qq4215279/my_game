package com.mumu.game.core.drop.consts;

import com.mumu.game.core.drop.item.DropItem;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DropMethod
 * 掉落方式
 * @author liuzhen
 * @version 1.0.0 2026/5/25 21:23
 */
public enum DropMethod {
    /** 必掉 */
    MUST_DROP(1) {
        @Override
        public List<DropItem> getFinalDropList(long playerId, List<DropItem> sourceDropList) {
            return sourceDropList;
        }
    },
    /** 单独概率（按照独立概率掉落） */
    SEPARATE_PROP(2) {
        @Override
        public List<DropItem> getFinalDropList(long playerId, List<DropItem> sourceDropList) {
            List<DropItem> list = new ArrayList<>();
            for (DropItem dropItem : sourceDropList) {
                if (dropItem.getProb() >= 1 || RandomUtils.nextDouble() < dropItem.getProb()) {
                        list.add(dropItem);
                    }
                }
            return list;
            }
        }
    ,
    /** 多选一 */
    MULTIPLE_CHOICE(3) {
        @Override
        public List<DropItem> getFinalDropList(long playerId, List<DropItem> sourceDropList) {
            List<DropItem> list = new ArrayList<>();
            // 按概率多选一
            DropItem dropItem = doDropRandom(sourceDropList);
            if (dropItem != null) {
                list.add(dropItem);
            }
            return list;
        }

        /**
         * 从掉落列表中按照概率随机选一个掉落
         * @param dropList dropList
         * @return com.mumu.game.core.drop.item.DropItem
         */
        private DropItem doDropRandom(List<DropItem> dropList) {
            double[] probs = new double[dropList.size()];
            int index = 0;
            double totalProb = 0;
            for (DropItem item : dropList) {
                probs[index++] = item.getProb();
                totalProb += item.getProb();
            }

            double prob = RandomUtils.nextDouble() * totalProb;
            double temp = 0;
            for (int i = 0; i < probs.length; i++) {
                temp += probs[i];
                if (prob < temp) {
                    return dropList.get(i);
                }
            }

            return null;
        }
    },

    ;

    /** 掉落方式 */
    private final int method;

    DropMethod(int method) {
        this.method = method;
    }

    /**
     * 获取最终掉落列表
     * @param playerId 玩家id
     * @param sourceDropList 源掉落列表
     * @return java.util.List<com.mumu.game.core.drop.item.DropItem>
     */
    public abstract List<DropItem> getFinalDropList(long playerId, List<DropItem> sourceDropList);
}
