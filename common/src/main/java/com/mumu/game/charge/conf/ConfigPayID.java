package com.mumu.game.charge.conf;

import lombok.Data;

/**
 * ConfigPayID
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 14:30
 */
@Data
public class ConfigPayID {

    /** 商品ID */
    private int goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 代币价格 */
    private int price;

    /** 支付ID$huawei */
    private String huawei;

    /** 支付ID$iOS */
    private String ios;

    /** 支付ID$googleplay */
    private String googleplay;

    /** 支付ID$prod */
    private String prod;
}
