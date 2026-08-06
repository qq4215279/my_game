package com.mumu.game.charge.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * ChargeInfo
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 13:47
 */
@Data
@Document("charge_info")
public class ChargeInfo {
    /** 订单id */
    @Id()
    private String orderId;
    /** 玩家id */
    private long playerId;
    /** 商品id */
    private int goodsId;
    /** 商品购买数量 */
    private int num;
    /** 支付渠道 */
    private String payChannel;
    /** 支付类型 */
    private String payType;
    /** 订单状态 */
    private int state;
    /** 支付ID */
    private String productId;
    /** 渠道商订单id */
    private String channelOrderId;
    /** 本次充值玩家支付的金额 */
    private int price;
    /** 实际支付时间 */
    private Date payTime;
    /** 订单创建时间 */
    private Date createTime;
    /** 支付成功信息 */
    private String payInfo;
    /** 额外参数 */
    private String extraInfo;
    /** 购买商品 */
    private int extraGoodsId;

}
