package com.mumu.game.charge.controller;

import java.util.List;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.dto.XsollaGoodsVO;
import com.mumu.game.charge.dto.XsollaPurchaseVO;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.charge.service.ChargeService;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.http.HttpCode;
import com.mumu.game.http.HttpResult;
import com.mumu.game.proto.charge.WCCreateOrderMessage;
import com.mumu.game.proto.message.core.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import jakarta.annotation.Resource;

/**
 * ChargeController
 * 游戏服充值Controller
 * @author liuzhen
 * @version 1.0.0 2024/11/26 11:20
 */
@RequestMapping("/charge")
@RestController
public class ChargeController {
  private LogTopic log = LogTopic.CHARGE;

  /** 艾克索拉（第三方支付） - 完成支付状态 */
  private static final int XSOLLA_FINISH_STATE = 1;

  @Resource
  PlayerManager playerManager;
  @Resource
  private ChargeService chargeService;
  @Resource
  private ChargeInfoDao chargeInfoDao;

  /**
   * 游戏服发货
   * @param orderId orderId
   * @param huaweiOrderId huaweiOrderId
   * @param purchaseToken purchaseToken
   * @param productId productId
   * @param accessToken accessToken
   * @return com.game.framework.http.common.HttpResult
   * @since 2024/11/26 20:19
   */
  @GetMapping("/chargeByHuawei")
  public HttpResult chargeByHuawei(@RequestParam String orderId, @RequestParam String huaweiOrderId, @RequestParam String purchaseToken,
                                   @RequestParam String productId, @RequestParam String accessToken) {

    log.info("ChargeController.charge", "orderId", orderId, "huaweiOrderId", huaweiOrderId, "purchaseToken", purchaseToken,
        "productId", productId, "accessToken", accessToken);

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      log.error("ChargeController.charge", "fail", "chargeInfo is not exist", "orderId", orderId);
      return HttpResult.error(HttpCode.ORDER_NOT_EXIST, "订单不存在");
    }

    // 转到玩家线程处理
    long playerId = chargeInfo.getPlayerId();
    /*MessageSender.sendRunNow(
        playerId, () -> chargeService.chargeByHuawei(playerId, orderId, huaweiOrderId, purchaseToken, productId, accessToken),
        "chargeByHuawei", playerId, orderId);*/

    return HttpResult.success();
  }

  /**
   * 苹果充值发货
   * @param orderId orderId
   * @param transactionId transactionId
   * @param sandbox sandbox
   * @return com.game.http.core.HttpResult
   * @since 2025/1/15 15:29
   */
  @GetMapping("/chargeByApple")
  public HttpResult chargeByApple(@RequestParam String orderId, @RequestParam String transactionId, @RequestParam boolean sandbox) {
    log.info("ChargeController.chargeByApple", "orderId", orderId, "transactionId", transactionId, "sandbox", sandbox);

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      log.error("ChargeController.charge", "fail", "chargeInfo is not exist", "orderId", orderId);
      return HttpResult.error(HttpCode.ORDER_NOT_EXIST, "订单不存在");
    }

    // 转到玩家线程处理
    long playerId = chargeInfo.getPlayerId();
    // MessageSender.sendRunNow(playerId, () -> chargeService.chargeByApple(playerId, orderId, transactionId, sandbox),
    //     "chargeByApple", playerId, orderId);

    return HttpResult.success();
  }

  /**
   * 谷歌充值发货
   * @param orderId orderId
   * @param purchaseToken purchaseToken
   * @return com.game.http.core.HttpResult
   * @since 2025/2/13 15:22
   */
  @GetMapping("/chargeByGoogle")
  public HttpResult chargeByGoogle(@RequestParam String orderId, @RequestParam String purchaseToken) {
    log.info("ChargeController.chargeByGoogle", "orderId", orderId, "purchasetoken", purchaseToken);

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      log.error("ChargeController.chargeByGoogle", "fail", "chargeInfo is not exist", "orderId", orderId);
      return HttpResult.error(HttpCode.ORDER_NOT_EXIST, "订单不存在");
    }

    // 转到玩家线程处理
    long playerId = chargeInfo.getPlayerId();
    // MessageSender.sendRunNow(playerId, () -> chargeService.chargeByGoogle(playerId, orderId, chargeInfo.getProductId(), purchaseToken),
    //     "chargeByGoogle", playerId, orderId);

    return HttpResult.success();
  }

  /**
   * 艾克索拉（第三方支付）充值发货
   * @param xsollaPurchaseVO playerId
   * @return com.game.http.core.HttpResult
   * @since 2025/6/12 10:28
   */
  @PostMapping("/chargeByXsolla")
  public HttpResult chargeByXsolla(@RequestBody XsollaPurchaseVO xsollaPurchaseVO) {
    long playerId = xsollaPurchaseVO.getPlayerId();
    log.info("ChargeController.chargeByXsolla", "playerId", playerId, "channelOrderId",
        xsollaPurchaseVO.getChannelOrderId(), "goodInfos", xsollaPurchaseVO.getGoodsDtoList());

    if (playerManager.notInServer(playerId)) {
      log.error("ChargeController.chargeByXsolla", "player not in server", "playerId", playerId, "channelOrderId");
      return HttpResult.error(HttpCode.FAIL, "玩家不在本服！" + playerId);
    }

    // 玩家id校验
    Player player = playerManager.getPlayerOrNullable(playerId);
    if (player == null) {
      log.error("ChargeController.chargeByXsolla", "player is null", "playerId", playerId, "channelOrderId");
      return HttpResult.error(HttpCode.FAIL, "玩家不存在！" + playerId);
    }

   /* MessageSender.sendRunNow(
        playerId,
        () -> {
          int channelOrderId = xsollaPurchaseVO.getChannelOrderId();
          String channelOrderIdStr = String.valueOf(channelOrderId);
          String payType = xsollaPurchaseVO.getPayType();
          List<XsollaGoodsVO> goodsDtoList = xsollaPurchaseVO.getGoodsDtoList();
          for (XsollaGoodsVO goodsDto : goodsDtoList) {
            if (goodsDto.getState() == XSOLLA_FINISH_STATE) {
              continue;
            }

            ChargeInfo chargeInfo = chargeInfoDao.getChargeInfoByChannelOrderId(playerId, goodsDto.getSku(), channelOrderIdStr);
            // 已发货
            if (chargeInfo != null && (chargeInfo.getState() == ChargeConstants.INIT_CHARGE_FINISH
                    || chargeInfo.getState() == ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH)) {
              continue;
            }

            int goodsId = goodsDto.getGoodsId();
            int num = goodsDto.getNum();
            String orderId = "";
            // 未创建订单
            if (chargeInfo == null) {
              ResponseResult createOrderResult = chargeService.createOrder(playerId, goodsId, num, ChargeConstants.INIT_CHARGE_SUCCESS, payType, "", 0, false);
              if (createOrderResult.isSuccess()) {
                WCCreateOrderMessage resMsg = (WCCreateOrderMessage) createOrderResult.getResMsg();
                orderId = resMsg.getOrderId();
              } else {
                log.error("ChargeController.chargeByXsolla", "createOrderFail", "playerId", playerId, "channelOrderId", "code", createOrderResult.getErrorCode());
              }

              // 创建订单，还未发货
            } else {
              orderId = chargeInfo.getOrderId();
            }

            // 发货
            if (StringUtils.isNotEmpty(orderId)) {
              ErrorCode errorCode = chargeService.chargeByXsolla(playerId, orderId, channelOrderIdStr);
              if (errorCode == ErrorCode.SUCCESS) {
                // 标记为已发货
                goodsDto.setState(XSOLLA_FINISH_STATE);
              }
            }
          }

          // 回调确认发货
          RestUtil.postForObject(ChargeConstants.XSOLLA_CONFIRMPURCHASE_SERVER_URL, new HttpEntity<>(xsollaPurchaseVO), HttpResult.class);
        },
        "chargeByXsolla", playerId, xsollaPurchaseVO.getChannelOrderId());*/

    return HttpResult.success().add("xsollaPurchaseVO", xsollaPurchaseVO);

  }
}
