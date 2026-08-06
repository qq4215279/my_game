package com.mumu.game.charge.action;

import com.mumu.game.charge.service.ChargeService;
import com.mumu.game.constants.ChargeConstants;
import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.cmd.anno.CmdMapping;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.proto.charge.CWChargeByFakeMessage;
import com.mumu.game.proto.charge.CWCreateOrderMessage;
import com.mumu.game.proto.charge.CWGetOrderInfoMessage;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Resource;

/**
 * ChargeAction
 * 充值action
 * @author liuzhen
 * @version 1.0.0 2024/9/26 16:25
 */
@CmdAction
public class ChargeAction {

  @Resource
  private ChargeService chargeService;

  /**
   * 请求创建订单
   * @param context context
   * @since 2024/9/26 17:50
   */
  @CmdMapping(Cmd.CWCreateOrder)
  public ResponseResult createOrder(MessageContext context) {
    long playerId = context.getPlayerId();
    CWCreateOrderMessage msg = context.getMsg(CWCreateOrderMessage.class);
    String payType = "";
    if (!StringUtils.isEmpty(msg.getPayType())) {
      payType = msg.getPayType();
    }
    String extraInfo = "";
    if (!StringUtils.isEmpty(msg.getExtraInfo())) {
      extraInfo = msg.getExtraInfo();
    }
    int extraGoodsId = 0;
    if (msg.getExtraGoodsId() != null) {
      extraGoodsId = msg.getExtraGoodsId();
    }

    return chargeService.createOrder(playerId, msg.getGoodsId(), 1, ChargeConstants.INIT_CHARGE_STATE, payType, extraInfo, extraGoodsId, true);
  }

  /**
   * 请求查询订单信息
   * @param context context
   * @since 2024/9/26 17:50
   */
  @CmdMapping(Cmd.CWGetOrderInfo)
  public ResponseResult getOrderInfo(MessageContext context) {
    long playerId = context.getPlayerId();
    CWGetOrderInfoMessage msg = context.getMsg(CWGetOrderInfoMessage.class);
    return chargeService.getOrderInfo(playerId, msg.getOrderId());
  }

  /**
   * 请求假购
   * @param context context
   * @since 2024/9/26 17:50
   */
  @CmdMapping(Cmd.CWChargeByFake)
  public ResponseResult chargeByFake(MessageContext context) {
    long playerId = context.getPlayerId();
    CWChargeByFakeMessage msg = context.getMsg(CWChargeByFakeMessage.class);
    return chargeService.chargeByFake(playerId, msg.getOrderId());
  }

}
