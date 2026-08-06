/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.enums;

import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.proto.charge.CWChargeByFakeMessage;
import com.mumu.game.proto.charge.CWCreateOrderMessage;
import com.mumu.game.proto.charge.CWGetOrderInfoMessage;
import com.mumu.game.proto.charge.WCChargeByFakeMessage;
import com.mumu.game.proto.charge.WCCreateOrderMessage;
import com.mumu.game.proto.charge.WCGetOrderInfoMessage;
import com.mumu.game.proto.message.gate.core.ConnectConfirmMsgCE;
import com.mumu.game.proto.message.gate.core.ConnectConfirmMsgEC;
import com.mumu.game.proto.message.gate.core.HeartbeatMsgCE;
import com.mumu.game.proto.message.gate.core.HeartbeatMsgEC;
import com.mumu.game.proto.message.server.ReconnectServerMsgAE;
import com.mumu.game.proto.message.server.ReconnectServerMsgEA;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

import com.mumu.game.proto.shop.CWBuyShopGoodsMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsByGoodsIdMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsByTypeMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsMessage;
import com.mumu.game.proto.shop.OnPushPopGoodsMessage;
import com.mumu.game.proto.shop.WCBuyShopGoodsMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsByGoodsIdMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsByTypeMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsMessage;
import lombok.Getter;

/**
 * Cmd 协议定义规范
 * 协议命名以收发方服务为前缀，如：CWEnterGame 表示 客户端 -> 大厅服的进入游戏请求
 * 前缀简写含义
 * C - Client 客户端
 * E - External 对外服，网关服
 * A - Any 任意服
 * W - World 大厅服
 * G - Game 游戏服
 * I - Chat 好友聊天服
 * Z - Center 中心服
 * @author liuzhen
 * @version 1.0.0 2025/3/30 12:49
 */
public enum Cmd implements ICmd {
    /** 空 */
    None(ServiceType.ALL, null, null),
    /** 心跳消息 */
    HeartbeatMsg(ServiceType.GATEWAY, HeartbeatMsgCE.class, HeartbeatMsgEC.class),
    /** 连接验证消息 */
    ConnectConfirmMsg(ServiceType.GATEWAY, ConnectConfirmMsgCE.class, ConnectConfirmMsgEC.class),
    /** 连接服务器 */
    ReconnectServerMsg(ServiceType.ALL, ReconnectServerMsgEA.class, ReconnectServerMsgAE.class),


    // --------------------- 玩家商城相关 ------------------------
    /** 请求商品信息列表 */
    CWGetShopGoods(ServiceType.WORLD, CWGetShopGoodsMessage.class, WCGetShopGoodsMessage.class),
    /** 请求商品信息列表ByType */
    CWGetShopGoodsByType(ServiceType.WORLD, CWGetShopGoodsByTypeMessage.class, WCGetShopGoodsByTypeMessage.class),
    /** 请求商品信息 */
    CWGetShopGoodsByGoodsId(ServiceType.WORLD, CWGetShopGoodsByGoodsIdMessage.class, WCGetShopGoodsByGoodsIdMessage.class),
    /** 请求购买商品 */
    CWBuyShopGoods(ServiceType.WORLD, CWBuyShopGoodsMessage.class, WCBuyShopGoodsMessage.class),
    // /** 请求发送推送弹窗礼包 */
    // AWSendOnPushPopGoods(AWSendOnPushPopGoodsMessage.class),
    /** 推送弹窗礼包 */
    OnPushPopGoods(ServiceType.WORLD, null, OnPushPopGoodsMessage.class),

    // --------------------- 玩家充值相关 ------------------------
    /** 请求创建订单 */
    CWCreateOrder(ServiceType.WORLD, CWCreateOrderMessage.class, WCCreateOrderMessage.class),
    /** 请求查询订单信息 */
    CWGetOrderInfo(ServiceType.WORLD, CWGetOrderInfoMessage.class, WCGetOrderInfoMessage.class),
    /** 请求假购 */
    CWChargeByFake(ServiceType.WORLD, CWChargeByFakeMessage.class, WCChargeByFakeMessage.class),
    ;

    /** 消息所属服务id组 */
    @Getter
    private final ServiceType serviceType;
    /** 请求协议消息结构体类型 */
    @Getter
    private final Class<?> reqMsgClass;
    /** 响应协议消息结构体类型 */
    @Getter
    private final Class<?> resMsgClass;

    Cmd(ServiceType serviceType, Class<?> reqMsgClass, Class<?> resMsgClass) {
        this.serviceType = serviceType;
        this.reqMsgClass = reqMsgClass;
        this.resMsgClass = resMsgClass;
    }

}
