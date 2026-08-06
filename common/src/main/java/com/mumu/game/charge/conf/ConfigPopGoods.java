package com.mumu.game.charge.conf;

import lombok.Data;

/**
 * 礼包弹窗表
 */
@Data
public class ConfigPopGoods {


  /** 主键 */
  private String data_id;

  /** 道具id */
  private String itemId;

  /** 商品id */
  private String goodsId;

  /** 购买商品可获得道具数量 */
  private int count;



}
