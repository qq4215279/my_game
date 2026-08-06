package com.mumu.game.charge.conf;

import lombok.Data;

/**
 * 商品表
 */
@Data
public class ConfigShop {


  /** 商品id */
  private int goodsId;

  /** 商品类型 */
  private int type;

  /** 商品名称 */
  private String name;

  /** 商品描述 */
  private String desc;

  /** 中文对照 */
  private String input_hBVlcK;

  /** 价格 */
  private String price;

  /** 原价 */
  private String originPrice;

  /** 折扣 */
  private int discount;

  /** 获得道具 */
  private String rewards;

  /** 额外获得 */
  private String extraRewards;

  /** 原奖励 */
  private String originRewards;

  /** 限购类型 */
  private String limitType;

  /** 限购数量 */
  private int limitCount;

  /** 商品排序 */
  private int sort;

  /** 商品图标 */
  private String icon;

  /** 角标 */
  private String tag;

  /** 礼包标题 */
  private String tile;

  /** 平台id */
  private String platform;

  /** 限定渠道 */
  private String channel;

  /** 额外参数 */
  private String param;

  /** 上架时间 */
  private long startBuyTime;

  /** 结束时间 */
  private long endBuyTime;
}
