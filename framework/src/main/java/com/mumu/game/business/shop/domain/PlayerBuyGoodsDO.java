package com.mumu.game.business.shop.domain;

import lombok.Data;

/**
 * PlayerBuyGoodsDO 玩家购买商品信息表
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/26 13:44
 */
@Data

public class PlayerBuyGoodsDO  {

  // @AutoColumn(name = "player_id", dbDefault = "0", primaryKey = true, order = 0, comment = "玩家id")
  private long playerId;

  // @AutoColumn(name = "goods_id", dbDefault = "0", primaryKey = true, order = 1, comment = "商品id")
  private int goodsId;

  // @AutoColumn(name = "count", dbDefault = "0", comment = "购买次数")
  private int count;

  // @AutoColumn(name = "total_count", dbDefault = "0", comment = "历史购买总次数")
  private int totalCount;

  // @AutoColumn(name = "last_reset_time", dbDefault = "0", comment = "上一次重置时间")
  private long lastResetTime;

  // @AutoColumn(name = "first_buy_time", dbDefault = "0", comment = "首次购买时间")
  private long firstBuyTime;

  // @AutoColumn(name = "last_buy_time", dbDefault = "0", comment = "上一次购买时间")
  private long lastBuyTime;


  /**
   * 增加购买数量
   *
   * @param add add
   * @since 2024/9/26 13:52
   */
  public void addCount(int add) {
    this.count += add;
    this.totalCount += add;

    this.lastBuyTime = System.currentTimeMillis();
  }

  /**
   * 重置购买次数
   *
   * @since 2024/9/26 13:53
   */
  public void reset() {
    this.count = 0;
    this.lastResetTime = System.currentTimeMillis();
  }
}
