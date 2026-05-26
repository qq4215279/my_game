package com.mumu.game.core.drop.item;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DropParams
 * // TODO
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:54
 */
public class DropParams {
    /** 是否需要弹出礼包 */
    public static final String NEED_POP = "needPop";

    /** 是否需要强制扣除（不校验库存数量，全部扣除） */
    public static final String FORCE_DEDUCT = "forceDeduct";

    /** 掉落相关扩展参数 */
    @Getter
    private final Map<String, Object> dropMap = new LinkedHashMap<>();

    // --------------------------- 程序内部的掉落相关额外参数，不上报数数 ---------------------------------
    /** 功能id */
    public static final String FUNCTION_ID = "functionId";
    /** 配置表名称 */
    public static final String CONFIG_NAME = "configName";
    /** 配置表主键id */
    public static final String CONFIG_ID = "configId";
    /** 礼物id */
    public static final String GIFT_ID = "giftId";
    /** 商品类型 */
    public static final String GOODS_TYPE = "goodsType";
    /** 商品id */
    public static final String GOODS_ID = "goodsId";
    /** 任务id */
    public static final String TASK_ID = "taskId";
    /** 邮件id */
    public static final String MAIL_ID = "mailId";
    /** 数量 */
    public static final String NUM = "num";
    /** 渠道id */
    public static final String CHANNEL = "channel";

    /** 空 */
    public static final DropParams EMPTY = new DropParams();

    public static DropParams build() {
        return new DropParams();
    }


    // --------------------------- 程序内部的掉落相关额外参数，不上报数数 ---------------------------------

    /** 设置强制扣除 */
    public DropParams forceDeduct() {
        dropMap.put(FORCE_DEDUCT, true);
        return this;
    }

    /** 是否强制扣除（默认不需要） */
    public boolean isForceDeduct() {
        return (boolean) dropMap.getOrDefault(FORCE_DEDUCT, false);
    }

    /** 设置需要弹出礼包 */
    public DropParams noNeedPop() {
        dropMap.put(NEED_POP, false);
        return this;
    }

    public DropParams needPop() {
        dropMap.put(NEED_POP, true);
        return this;
    }

    /** 是否需要弹出礼包（默认需要） */
    public boolean isNeedPopGoods() {
        return (boolean) dropMap.getOrDefault(NEED_POP, true);
    }

}
