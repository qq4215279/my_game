/*
 * Copyright 2020-2026, mumu without 996. All Right Reserved.
 */

package com.mumu.game.core.utils;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.mumu.game.constants.CoreConstants;
import com.mumu.game.constants.Symbol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * RandomUtils 随机工具类
 * 
 * @author liuzhen
 * @version 1.0.0 2024/8/15 17:11
 */
public class RandomUtils extends cn.hutool.core.util.RandomUtil {


    /** 必中 */
    public static final int SURE_HIT = -1;

    /** 精度-百分比 */
    public static final int SCALE_100 = 100;

    /**
     * 概率命中
     *
     * @param prob 概率值 [0,scale]，特殊的，-1表示必中
     * @param scale 精度范围
     * @return true-命中
     */
    public static boolean hit(int prob, int scale) {
        if (prob == SURE_HIT || prob >= scale) return true;
        return randomInt(scale) < prob;
    }

    /** 概率命中，万分比 */
    public static boolean hit10000(int prob) {
        return hit(prob, CoreConstants.GAME_TOTAL_RATE);
    }

    /** 概率命中，百分比 */
    public static boolean hit(int prob) {
        return hit(prob, SCALE_100);
    }

    /** 从[min, max]中随机一个Int值（注，与randomInt的[min, max)随机范围不同） */
    public static int rollInt(int min, int max) {
        return randomInt(Math.min(min, max), Math.max(min, max) + 1);
    }

    /** 从[1, max]中随机一个值（注，与randomInt的[min, max)随机范围不同） */
    public static int rollInt(int max) {
        return max < 1 ? 0 : rollInt(1, max);
    }

    /** 从int数组中随机一个值 */
    public static int rollInt(int[] range) {
        if (range.length != 2) return 0;
        return rollInt(range[0], range[1]);
    }

    /** 从[min, max]中随机一个Long值（注，与randomInt的[min, max)随机范围不同） */
    public static long rollLong(long min, long max) {
        return randomLong(Math.min(min, max), Math.max(min, max) + 1);
    }

    /** 从[1, max]中随机一个值（注，与randomInt的[min, max)随机范围不同） */
    public static long rollLong(long max) {
        return max < 1 ? 0 : rollLong(1, max);
    }

    /** 随机一个元素（list为null时不会报错） */
    public static <T> T rollEle(Collection<T> coll) {
        return CollUtil.isEmpty(coll)
                ? null
                : coll instanceof List list ? (T) randomEle(list) : randomEle(CollUtil.distinct(coll));
    }

    /**
     * 权重随机
     *
     * @param weightArr 权重数组，每个元素值为权重
     * @return int 返回值为数组下标
     * @since 2025/4/23 18:11
     */
    public static int weightRandom(int[] weightArr) {
        if (weightArr == null || weightArr.length == 0) {
            return 0;
        }

        int total = Arrays.stream(weightArr).sum();

        int rand = ThreadLocalRandom.current().nextInt(total) + 1;
        int cursor = 0;
        for (int i = 0; i < weightArr.length; i++) {
            int weight = weightArr[i];

            if (weight < 1) {
                continue;
            }

            cursor += weight;
            if (cursor >= rand) {
                return i;
            }
        }

        return 0;
    }

    /**
     * 权重随机
     *
     * @param weightMap weightMap key: 值; value: 权重
     * @return int 返回key
     * @since 2025/4/23 18:13
     */
    public static <T> T weightRandom(Map<T, Integer> weightMap) {
        if (weightMap.isEmpty()) {
            return null;
        }

        int total = weightMap.values().stream().mapToInt(i -> i).sum();

        int rand = ThreadLocalRandom.current().nextInt(total) + 1;
        int cursor = 0;

        for (Map.Entry<T, Integer> entry : weightMap.entrySet()) {
            T object = entry.getKey();
            int weight = entry.getValue();

            if (weight < 1) {
                continue;
            }

            cursor += weight;
            if (cursor >= rand) {
                return object;
            }
        }

        return null;
    }

    /**
     * 权重随机
     *
     * @param weightStr 权重字符串（注：多个分号分割，单个逗号分割，每一位必须是int类型，解析最后一位是权重） eg: 1,1,1000;2,2,2400;3,3,5000 =>
     *     value1,value2,权重;value1,value2,权重;
     * @return com.game.framework.core.utils.RandomUtils.WeightOB
     * @since 2025/4/21 18:07
     */
    public static WeightBO weightRandom(String weightStr) {
        if (StringUtils.isEmpty(weightStr)) {
            return null;
        }

        List<WeightBO> weights = parseWeightStr(weightStr);
        return weightRandom(weights);
    }

    /**
     * 权重随机
     *
     * @param weights 权重字符串（注：多个分号分割，单个逗号分割，每一位必须是int类型，解析最后一位是权重）
     * @return com.game.framework.core.utils.RandomUtils.WeightOB
     * @since 2025/4/22 14:52
     */
    public static WeightBO weightRandom(List<WeightBO> weights) {
        if (weights.isEmpty()) {
            return null;
        }

        int total = 0;
        for (WeightBO weightBO : weights) {
            total += weightBO.weight;
        }

        int rand = ThreadLocalRandom.current().nextInt(total) + 1;
        int cursor = 0;
        for (WeightBO weightBO : weights) {
            int weight = weightBO.weight;
            if (weight < 1) {
                continue;
            }

            cursor += weight;
            if (cursor >= rand) {
                return weightBO;
            }
        }

        return null;
    }

    /**
     * 随机权重
     *
     * @param weights weights
     * @param weightFunc weightFunc
     * @return T
     * @since 2025/5/27 17:55
     */
    public static <T> T weightRandom(List<T> weights, Function<T, Integer> weightFunc) {
        if (weights.isEmpty()) {
            return null;
        }

        int total = 0;
        for (T t : weights) {
            total += weightFunc.apply(t);
        }

        int rand = ThreadLocalRandom.current().nextInt(total) + 1;
        int cursor = 0;
        for (T t : weights) {
            int weight = weightFunc.apply(t);
            if (weight < 1) {
                continue;
            }

            cursor += weight;
            if (cursor >= rand) {
                return t;
            }
        }

        return null;
    }

    /** 解析权重字符串 weightStr 权重字符串（注：多个分号分割，单个逗号分割，每一位必须是int类型，解析最后一位是权重） */
    public static List<WeightBO> parseWeightStr(String weightStr) {
        List<WeightBO> weights = new ArrayList<>();

        String[] split = weightStr.split(Symbol.SEMICOLON);
        for (String s : split) {
            String[] split1 = s.split(Symbol.COMMA);
            int len = split1.length;
            int value = len > 1 ? Integer.parseInt(split1[0]) : 0;
            int value2 = len > 2 ? Integer.parseInt(split1[1]) : 0;
            int weight = Integer.parseInt(split1[len - 1]);

            weights.add(new WeightBO(value, value2, weight));
        }

        return weights;
    }

    @AllArgsConstructor
    @ToString
    @Getter
    public static class WeightBO {
        /** index0值 */
        private long value;

        /** index1值 */
        private int value2;

        /** 权重 */
        private int weight;

        public WeightBO(long value, int weight) {
            this.value = value;
            this.value2 = 0;
            this.weight = weight;
        }
    }

    public static void main(String[] args) {
        // for (int i = 0; i < 100; i++) {
        //   System.out.println(rollInt(0, -1));
        // }

        int[] weightArr = {1000, 2000, 3000, 1000, 5000};
        Map<Integer, Integer> weightMap = Map.of(1, 100, 2, 200, 3, 300, 10, 1000, 4, 200);
        for (int i = 0; i < 10; i++) {
            // WeightOB weightOB = weightRandom("1,1,1000;2,2,2400;3,3,5000");
            // System.out.println(weightOB);

            // System.out.println(weightRandom(weightArr));
            // System.out.println(weightRandom(weightMap));
        }

        System.out.println(rollEle(List.of(1, 2, 3, 4)));
        System.out.println(rollEle(Map.of(1, 1, 2, 2, 3, 3).keySet()));
        System.out.println(rollEle(Lists.<String>newArrayList()));
    }
}
