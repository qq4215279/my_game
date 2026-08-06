package com.mumu.game.charge.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * XsollaPurchaseVO
 * @author liuzhen
 * @version 1.0.0 2025/6/12 11:07
 */
@Data
@AllArgsConstructor
public class XsollaPurchaseVO {
  /** 玩家id */
  private long playerId;
  /** 第三方订单id */
  private int channelOrderId;
  /** 支付方式 */
  private String payType;
  /** 购买商品列表 */
  private List<XsollaGoodsVO> goodsDtoList;
}
