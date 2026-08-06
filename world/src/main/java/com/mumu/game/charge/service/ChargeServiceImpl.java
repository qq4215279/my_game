package com.mumu.game.charge.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.business.player.enums.ChannelEnum;
import com.mumu.game.business.shop.luban.ShopConfigManager;
import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.core.utils.HttpUtils;
import com.mumu.game.proto.charge.WCCreateOrderMessage;
import com.mumu.game.proto.charge.WCGetOrderInfoMessage;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.shop.service.ShopService;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import jakarta.annotation.Resource;

/**
 * ChargeServiceImpl 充值service
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/26 16:25
 */
@Service()
public class ChargeServiceImpl implements ChargeService {

  private static final LogTopic log = LogTopic.CHARGE;

  @Resource private PlayerManager playerManager;
  @Resource
  ShopService shopService;
  @Resource
  ChargeInfoDao chargeInfoDao;

  @Override
  public ResponseResult createOrder(
      long playerId, int goodsId, int num, int state, String payType, String extraInfo, int extraGoodsId, boolean needCheckBuy) {
    /*if (StringUtils.isEmpty(channel) || StringUtils.isEmpty(payType)) {
      MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_PARAM_ERROR);
      return;
    }*/

    /*ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);
    if (configShop == null) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_GOODS_NOT_EXIST);
    }

    // 购买校验
    if (needCheckBuy) {
      ErrorCode errorCode = shopService.checkBuyGoods(playerId, num, configShop);
      if (errorCode != ErrorCode.SUCCESS) {
        return ResponseResult.error(playerId, errorCode);
      }
    }

    // 不是计费点商品
    long currencyNum = RewardEnum.CURRENCY.getNum(configShop.getPrice());
    if (currencyNum <= 0) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_CHECK_BUY_CONDITION);
    }

    // TODO 玩家总充值上限检查
    // TODO 玩家当天充值上限检查
    // TODO 检查防沉迷充值金额限额

    PlayerBaseDO playerBaseDO = PlayerBaseDOOperator.self().getPlayerBaseDO(playerId);
    String channel = playerBaseDO.getLastChannel();
    String productId = ChannelEnum.getChannelEnum(channel).getProductId(goodsId);

    // 创建订单
    // ItemBean priceBean = shopService.getPriceBean(configShop);
    ItemBean priceBean = configShop.getPriceBean();
    int price = Integer.parseInt(String.valueOf(priceBean.getNum()));
    String orderId = String.valueOf(SnowflakeID.nextId());
    // todo 客户端连续点击商品，会瞬时创建多比订单，占用数据库资源
    chargeInfoDao.insertChargeInfo(orderId, playerId, goodsId, num, channel, payType, state,
        productId, price, new Date(), extraInfo, extraGoodsId);
    log.info(playerId, "createOrder", "goodsId", goodsId, "channel", channel, "payType", payType,
        "productId", productId, "price", price, "extraInfo", extraInfo, "extraGoodsId", extraGoodsId);

    statisticOrder(PlayerManager.self().getPlayer(playerId), orderId, goodsId, price, extraInfo);*/

    // return
    WCCreateOrderMessage resMsg = new WCCreateOrderMessage();
    // resMsg.setOrderId(orderId);
    return ResponseResult.success(playerId, resMsg);
  }

  /**
   * 充值统计
   *
   * @param player player
   * @param orderId orderId
   * @param goodsId goodsId
   * @param price price
   * @since 2025/3/7 09:38
   */
  private void statisticOrder(Player player, String orderId, int goodsId, int price, String extraInfo) {
    long playerId = player.getPlayerId();
    /*PlayerBaseDO playerBaseDO = PlayerBaseDOOperator.self().getPlayerBaseDO(playerId);

    // 充值时，统计玩家日志
    StatisticManager.record(
        new PlayerOrderStatisticPo()
            .setOrderId(orderId)
            .setGoodsId(goodsId)
            .setPrice(price)
            .setExtraInfo(extraInfo)
            .setPlayerId(playerId)
            .setRobot(PlayerUtil.getRobot(playerId))
            .setChannel(playerBaseDO.getChannel())
            .setLastChannel(playerBaseDO.getLastChannel())
            .setRegisterPlayer(PlayerUtil.getRegisterPlayer(playerId)));

    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);

    // 报表统计创建订单日志
    StatisticFormUtil.logKvs(
        StatisticFromType.CHARGE,
        "orderId",
        orderId,
        "playerId",
        playerId,
        "goodsId",
        goodsId,
        "goodsName",
        configShop.getName(),
        "price",
        price,
        "flag",
        0, // 发起支付
        "registerPlayer",
        PlayerUtil.getRegisterPlayer(playerId),
        "robot",
        PlayerUtil.getRobot(playerId),
        "channel",
        playerBaseDO.getChannel(),
        "extraInfo",
        extraInfo);*/

  }

  @Override
  public ResponseResult getOrderInfo(long playerId, String orderId) {
    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_ORDER_NOT_EXIST);
    }
    if (chargeInfo.getPlayerId() != playerId) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_ORDER_NOT_EXIST);
    }

    // return
    WCGetOrderInfoMessage resMsg = new WCGetOrderInfoMessage();
    resMsg.setGoodsId(chargeInfo.getGoodsId());
    resMsg.setChannel(chargeInfo.getPayChannel());
    resMsg.setPayType(chargeInfo.getPayType());
    resMsg.setExtraInfo(chargeInfo.getExtraInfo());
    resMsg.setExtraGoodsId(chargeInfo.getExtraGoodsId());
    return ResponseResult.success(playerId, resMsg);
  }

  @Override
  public void chargeByHuawei(
      long playerId,
      String orderId,
      String huaweiOrderId,
      String purchaseToken,
      String productId,
      String accessToken) {
    boolean chargeSuccess = false;

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);

    // 未充值成功
    if (chargeInfo.getState() != ChargeConstants.INIT_CHARGE_SUCCESS) {
      log.error("chargeByHuawei", "fail, 未充值成功", "orderId", orderId);
      return;
    }

    Map<String, String> params = new HashMap<>(3);
    params.put("huaweiOrderId", huaweiOrderId);
    params.put("purchaseToken", purchaseToken);
    params.put("accessToken", accessToken);
    String payInfo = JSON.toJSONString(params);
    chargeInfoDao.updatePayInfo(orderId, payInfo);

    // 发货
    try {
      chargeSuccess = doCharge(playerId, orderId, ChannelEnum.HUAWEI);
    } catch (Exception e) {
      log.error(e, "chargeByHuawei", "fail", "失败", "orderId", orderId);
    } finally {
      if (!chargeSuccess) {
        log.error("chargeByHuawei", "fail", "失败", "orderId", orderId);
      }
    }

    // 通知华为，发货成功
    //  TODO 写死 账号服地址
    if (chargeSuccess) {
      HttpUtils.asyncGet(
          ChargeConstants.CONFIRMPURCHASE_SERVER_URL,
          Map.of(
              "orderId",
              orderId,
              "purchaseToken",
              purchaseToken,
              "productId",
              productId,
              "accessToken",
              accessToken),
          (result, err) -> {
            if (err != null) {
              log.error(
                  err,
                  "confirmPurchase http error",
                  "playerId",
                  playerId,
                  "orderId",
                  orderId,
                  "purchaseToken",
                  purchaseToken,
                  "productId",
                  productId,
                  "accessToken",
                  accessToken);
            }
          });
    }
  }

  @Override
  public void chargeByApple(long playerId, String orderId, String transactionId, boolean sandbox) {
    boolean chargeSuccess = false;

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    // 未充值成功
    if (chargeInfo.getState() != ChargeConstants.INIT_CHARGE_SUCCESS) {
      log.error("chargeByApple", "fail, 未充值成功", "orderId", orderId);
      return;
    }

    Map<String, String> params = new HashMap<>(3);
    params.put("transactionId", transactionId);
    params.put("sandbox", String.valueOf(sandbox));
    String payInfo = JSON.toJSONString(params);
    chargeInfoDao.updatePayInfo(orderId, payInfo);

    // 发货
    try {
      chargeSuccess = doCharge(playerId, orderId, ChannelEnum.IOS);
    } catch (Exception e) {
      log.error(e, "chargeByApple", "fail", "失败", "orderId", orderId);
    } finally {
      if (!chargeSuccess) {
        log.error("chargeByApple", "fail", "失败", "orderId", orderId);
      }
    }

    // 通知苹果，发货成功
    //  TODO 写死 账号服地址
    if (chargeSuccess) {
      HttpUtils.asyncGet(
          ChargeConstants.APPLE_CONFIRMPURCHASE_SERVER_URL,
          Map.of(
              "orderId",
              orderId,
              "playerId",
              playerId,
              "transactionId",
              transactionId,
              "sandbox",
              sandbox),
          (result, err) -> {
            if (err != null) {
              log.error(
                  err,
                  "confirmPurchase http error",
                  "playerId",
                  playerId,
                  "orderId",
                  orderId,
                  "transactionId",
                  transactionId,
                  "sandbox",
                  sandbox);
            }
          });
    }
  }

  @Override
  public void chargeByGoogle(
      long playerId, String orderId, String productId, String purchaseToken) {
    boolean chargeSuccess = false;

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    // 未充值成功
    if (chargeInfo.getState() != ChargeConstants.INIT_CHARGE_SUCCESS) {
      log.error("chargeByGoogle", "fail, 未充值成功", "orderId", orderId);
      return;
    }

    Map<String, String> params = new HashMap<>(3);
    params.put("purchaseToken", purchaseToken);
    String payInfo = JSON.toJSONString(params);
    chargeInfoDao.updatePayInfo(orderId, payInfo);

    // 发货
    try {
      chargeSuccess = doCharge(playerId, orderId, ChannelEnum.GOOGLEPLAY);
    } catch (Exception e) {
      log.error(e, "chargeByGoogle", "fail", "失败", "orderId", orderId);
    } finally {
      if (!chargeSuccess) {
        log.error("chargeByGoogle", "fail", "失败", "orderId", orderId);
      }
    }

    // 通知谷歌，发货成功
    //  TODO 写死 账号服地址
    if (chargeSuccess) {
      HttpUtils.asyncGet(
          ChargeConstants.GOOGLE_CONFIRMPURCHASE_SERVER_URL,
          Map.of(
              "orderId",
              orderId,
              "playerId",
              playerId,
              "productId",
              productId,
              "purchaseToken",
              purchaseToken),
          (result, err) -> {
            if (err != null) {
              log.error(
                  err,
                  "confirmPurchase http error",
                  "playerId",
                  playerId,
                  "orderId",
                  orderId,
                  "productId",
                  productId,
                  "purchaseToken",
                  purchaseToken);
            }
          });
    }
  }

  @Override
  public ErrorCode chargeByXsolla(long playerId, String orderId, String channelOrderId) {
    boolean chargeSuccess = false;

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    // 未充值成功
    if (chargeInfo.getState() != ChargeConstants.INIT_CHARGE_SUCCESS) {
      log.error("chargeByXsolla", "fail, 未充值成功", "orderId", orderId);
      return ErrorCode.FAIL;
    }

    // 发货
    try {
      chargeSuccess = doCharge(playerId, orderId, ChannelEnum.PROD);
      if (chargeSuccess) {
        // 标记发货成功
        chargeInfoDao.updateState(orderId, ChargeConstants.INIT_CHARGE_NOTIFY_THIRD_FINISH, new Date());
      }

    } catch (Exception e) {
      log.error(e, "chargeByXsolla", "fail", "失败", "orderId", orderId);
    } finally {
      if (!chargeSuccess) {
        log.error("chargeByXsolla", "fail", "失败", "orderId", orderId);
      }
    }

    return ErrorCode.SUCCESS;
  }

  @Override
  public ResponseResult chargeByFake(long playerId, String orderId) {
    // 非测试环境
    /*if (ConfigSwitchEnum.notTest()) {
      return ResponseResult.error(playerId, ErrorCode.ILLEGAL);
    }*/

    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
    if (chargeInfo == null) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_ORDER_NOT_EXIST);
    }
    if (chargeInfo.getPlayerId() != playerId) {
      log.error("checkOrder", "订单不存在");
      return ResponseResult.error(playerId, ErrorCode.FAIL_ORDER_NOT_EXIST);
    }

    // 支付成功表示
    chargeInfoDao.updateState(orderId, ChargeConstants.INIT_CHARGE_SUCCESS, new Date());

    /*if (chargeInfo.getState() != ChargeConstants.INIT_CHARGE_SUCCESS) {
      log.error("charge", "fail", "playerId", playerId, "orderId", orderId, "订单充值失败");
      MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_CHECK_BUY_CONDITION);
      return;
    }*/

    log.info(playerId, "chargeByFake", "orderId", orderId);

    // 假购充值
    try {
      doCharge(playerId, orderId, ChannelEnum.DEV);
    } catch (Exception e) {
      log.error(e, "chargeByFake", "fail", "假购失败", "orderId", orderId);
    }

    return ResponseResult.success(playerId);
  }

  @Override
  public boolean doCharge(long playerId, String orderId, ChannelEnum chargeChannel) {
    ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);

    // 充值消费事件
    Player player = playerManager.getPlayer(playerId);
    // WorldActionUtil.chargeTrigger(player, chargeInfo.getPrice());

    // 增加vip经验
    // Drop drop = Drop.of(ConfigPlayerParamsEnum.RMB_CONVERT_VIP_EXP.toStr());
    // drop.multi(chargeInfo.getPrice() * chargeInfo.getNum());
    // drop.rewardItem(player, CurrencyAction.CHARGE, DropParams.build().putExpArg(orderId).setNum(chargeInfo.getNum()).setChannel(chargeChannel.getChannel()));

    ErrorCode chargeCode = ErrorCode.FAIL;
    boolean chargeSuccess = false;
    try {
      chargeCode = shopService.busShopGoods(playerId, chargeInfo.getGoodsId(), chargeInfo.getNum(), true);
      chargeSuccess = chargeCode == ErrorCode.SUCCESS;
    } catch (Exception e) {
      log.error(
          e,
          "charge",
          "fail",
          "playerId",
          playerId,
          "orderId",
          orderId,
          "goodsId",
          chargeInfo.getGoodsId(),
          "chargeCode",
          chargeCode);
    } finally {
      if (!chargeSuccess) {
        log.error(
            "charge",
            "fail",
            "playerId",
            playerId,
            "orderId",
            orderId,
            "goodsId",
            chargeInfo.getGoodsId(),
            "chargeCode",
            chargeCode);
      }
    }

    // 充值成功
    if (chargeSuccess) {
      chargeInfoDao.updateState(orderId, ChargeConstants.INIT_CHARGE_FINISH, new Date());
      log.info(playerId, "charge", "success", "orderId", orderId);
    }

    int extraGoodsId = chargeInfo.getExtraGoodsId();
    // 充值成功，在购买商品
    if (chargeSuccess && extraGoodsId > 0) {
      ErrorCode extraChargeCode = ErrorCode.FAIL;
      try {
        extraChargeCode = shopService.busShopGoods(playerId, extraGoodsId, chargeInfo.getNum(), false);
      } catch (Exception e) {
        log.error(
            e,
            "charge",
            "busShopGoods fail",
            "playerId",
            playerId,
            "orderId",
            orderId,
            "extraGoodsId",
            extraGoodsId,
            "chargeCode",
            extraChargeCode);
      } finally {
        if (extraChargeCode != ErrorCode.SUCCESS) {
          log.error(
              "charge",
              "busShopGoods fail",
              "playerId",
              playerId,
              "orderId",
              orderId,
              "extraGoodsId",
              extraGoodsId,
              "chargeCode",
              extraChargeCode);
        }
      }
    }

    // 玩家充值统计
    statisticCharge(player, orderId, chargeInfo.getGoodsId(), chargeInfo.getPrice(), chargeChannel);

    return chargeSuccess;
  }

  /**
   * 充值统计
   *
   * @param player player
   * @param orderId orderId
   * @param goodsId goodsId
   * @param price price
   * @param chargeChannel chargeChannel
   * @since 2025/3/7 09:38
   */
  private void statisticCharge(Player player, String orderId, int goodsId, int price, ChannelEnum chargeChannel) {
    long playerId = player.getPlayerId();
    // PlayerBaseDO playerBaseDO = PlayerBaseDOOperator.self().getPlayerBaseDO(playerId);

    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);

    /*// 充值时，统计玩家日志
    StatisticManager.record(
        new PlayerChargeStatisticPo()
            .setOrderId(orderId)
            .setGoodsId(goodsId)
            .setPrice(price)
            .setRegisterPlayer(PlayerUtil.getRegisterPlayer(playerId))
            .setPlayerId(playerId)
            .setChannel(playerBaseDO.getChannel())
            .setRobot(PlayerUtil.getRobot(playerId))
            .setLastChannel(playerBaseDO.getLastChannel()));
    // 报表统计充值日志
    StatisticFormUtil.logKvs(
        StatisticFromType.CHARGE,
        "orderId",
        orderId,
        "playerId",
        playerId,
        "goodsId",
        goodsId,
        "goodsName",
        configShop.getName(),
        "price",
        price,
        "flag",
        1, // 充值完成
        "registerPlayer",
        PlayerUtil.getRegisterPlayer(playerId),
        "robot",
        PlayerUtil.getRobot(playerId),
        "channel",
        playerBaseDO.getChannel());*/
  }
}
