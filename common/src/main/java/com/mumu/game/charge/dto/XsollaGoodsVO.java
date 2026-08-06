package com.mumu.game.charge.dto;


import com.mumu.game.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XsollaGoodsDto
 * 第三方支付商品信息dto
 * @author liuzhen
 * @version 1.0.0 2025/6/11 17:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XsollaGoodsVO {
  /** 商品id */
  private int goodsId;
  /** sku */
  private String sku;
  /** 商品购买数量 */
  private int num;
  /** 发货状态: 0-未发货; 1-已发货 */
  private int state;

  @Override
  public String toString() {
    return JsonUtil.toJson(this);
  }
}
