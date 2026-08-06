package com.mumu.game.account.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * XsollaChargeInfo
 * 第三方支付订单信息
 * @author liuzhen
 * @version 1.0.0 2025/6/11 15:12
 */
@Data
@AllArgsConstructor
@TableName("xsolla_charge_info")
public class XsollaChargeInfo {
  /** 渠道商订单id（艾克索拉侧用户订单的唯一标识符）- channelOrderId */
  @TableId()
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
  private String goodsInfos;
  /** 订单创建时间 */
  private Date createTime;
  /** 额外参数 */
  private String extraInfo;
  /** 上一次检查时间 */
  private Date lastCheckTime;
}
