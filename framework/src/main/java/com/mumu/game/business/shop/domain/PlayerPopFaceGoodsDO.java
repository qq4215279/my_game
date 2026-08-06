package com.mumu.game.business.shop.domain;

import lombok.Data;

/**
 * PlayerPopFaceGoodsDO
 * 玩家弹脸礼包信息
 * @author liuzhen
 * @version 1.0.0 2025/7/8 16:21
 */
@Data
public class PlayerPopFaceGoodsDO {
  /** 玩家id */
  private long playerId;
  /** 商品id */
  private int goodsId;
  /** 弹脸时间 */
  private long popFaceTime;
  /** 是否有红点（首次弹出红点） */
  private boolean hasPoint;
  /** 触发任务id */
  private int triggerTaskId;

}
