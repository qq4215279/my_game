/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.log;

/**
 * CurrencyAction
 * 货币流通事件，用于日志
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:08
 */
public enum CurrencyAction {
    /** 绑定账号 */
    BIND_ACCOUNT,
    /** 购买商品 */
    BUY_SHOP,
    /** 购买礼物商品 */
    BUY_GIFT_SHOP,
    /** GM */
    GM,
    /** 创角发放默认道具 */
    CREATE_PLAYER_SEND_DEFAULT_ITEM,
    /** 玩家升级 */
    PLAYER_UPGRADE,
    /** 玩家VIP升级 */
    PLAYER_VIP_UPGRADE,
    /** 改名消耗 */
    UPDATE_NICK_COST,
    /** 使用道具 */
    USE_ITEM_COST,
    /** 邮件领奖 */
    MAIL,
    /** 任务 */
    TASK,
    /** 假购 */
    FAKE_CHARGE,
    /** 充值 */
    CHARGE,
    DROP;
}
