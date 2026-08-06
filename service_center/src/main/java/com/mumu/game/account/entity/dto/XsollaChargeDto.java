package com.mumu.game.account.entity.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import com.mumu.game.charge.dto.XsollaGoodsVO;
import lombok.Data;

/**
 * XsollaChargeDto
 * @author liuzhen
 * @version 1.0.0 2025/6/11 17:15
 */
@Data
public class XsollaChargeDto {
  /** 渠道商订单id（艾克索拉侧用户订单的唯一标识符）- channelOrderId */
  private int id;
  /** 玩家id */
  private long playerId;
  /** 付款模式。对于真实支付，使用default；对于测试性支付，使用sandbox。 */
  private String mode;
  /** 支付货币类型。real-真实货币; unknown-免费订单; virtual-虚拟货币 */
  private String currencyType;
  /** 购物车总价 */
  private String amount;
  /** 发票ID */
  private String invoiceId;
  /** 订单状态 0: 未全部发货; 1: 全部已发货 */
  private int state;
  /** 购买商品信息: sku(支付ID):数量:状态(0-未发货;1-已发货);sku:数量:状态(0-未发货;1-已发货); */
  private List<XsollaGoodsVO> goods = new ArrayList<>();
  /** 订单创建时间 */
  private Date createTime;
  /** 额外参数 */
  private String extraInfo;

  public XsollaChargeDto(int id, long playerId, String mode, String currencyType, String amount,
      String invoiceId, int state, Date createTime, String extraInfo) {
    this.id = id;
    this.playerId = playerId;
    this.mode = mode;
    this.currencyType = currencyType;
    this.amount = amount;
    this.invoiceId = invoiceId;
    this.state = state;
    this.createTime = createTime;
    this.extraInfo = extraInfo;
  }

}
