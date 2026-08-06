package com.mumu.game.template.shop.core.template;

import java.util.List;
import java.util.Objects;


import cn.hutool.core.util.BooleanUtil;
import com.mumu.game.business.shop.domain.PlayerBuyGoodsDO;
import com.mumu.game.business.shop.luban.ShopConfigManager;
import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.business.shop.operator.PlayerBuyGoodsDOOperator;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropParams;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.shop.GoodsBean;
import com.mumu.game.proto.shop.WCBuyShopGoodsMessage;
import com.mumu.game.template.func.core.FunctionId;
import com.mumu.game.template.func.core.temp.Template;
import jakarta.annotation.Resource;
import lombok.Setter;

/**
 * AbstractShopTemp 抽象商品模版
 *
 * @author liuzhen
 * @version 1.0.0 2024/11/19 20:13
 */
public abstract class AbstractShopTemp implements ShopTemp {
  protected static final LogTopic log = LogTopic.ACTION;

  /** 商品类型 */
  @Setter protected int goodsType;

  @Resource protected PlayerManager playerManager;
  @Resource protected PlayerBuyGoodsDOOperator playerBuyGoodsDOOperator;
  @Resource protected PlayerTemplateDOOprator playerTemplateDOOprator;

  @Override
  public final boolean checkRedPoint(long playerId) {
    return ShopConfigManager.getConfigShopList(goodsType).stream()
        .anyMatch(conf -> checkRedPoint(playerId, conf));
  }

  /** 检查礼包红点 */
  protected boolean checkRedPoint(long playerId, ConfigShopDTO configShop) {
    // 非免费商品
    if (!configShop.isFree()) {
      return false;
    }
    // TODO 开始时间 截止时间 校验 configShop.getStartBuyTime() configShop.getEndBuyTime()
    // 购买上限
    if (configShop.notStock(playerId)) {
      return false;
    }
    return true;
  }

  @Override
  public final ErrorCode checkBuyGoods(long playerId, int goodsId, int num) {
    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);
    if (configShop == null) {
      return ErrorCode.FAIL_GOODS_NOT_EXIST;
    }

    // TODO 开始时间 截止时间 校验 configShop.getStartBuyTime() configShop.getEndBuyTime()

    // 1. 购买上限
    if (configShop.notStock(playerId, num)) {
      return ErrorCode.FAIL_BUY_LIMIT;
    }

    // 2. 功能模版处理 checkBuyGoods
    int functionId = ShopConfigManager.getFunctionId(goodsType);
    Template template = FunctionId.loadFuncTemplate(playerId, functionId);
    if (!template.isOpen(playerId)) {
      return ErrorCode.FAIL_FUNCTION_NOT_OPOEN;
    }

    return checkBuyGoods(playerId, configShop);
  }

  /** 购买商品检查（子类扩展） */
  protected ErrorCode checkBuyGoods(long playerId, ConfigShopDTO configShop) {
    return ErrorCode.SUCCESS;
  }

  @Override
  public final ErrorCode busShopGoods(
      long playerId, int goodsId, int num, boolean rmbBuy, WCBuyShopGoodsMessage resMsg) {
    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);

    // 购买条件Price: 1. 免费  2. 金币  3. 钻石  4. 人民币  5. 广告
    Drop costDrop = Drop.of(configShop.getPrice()).multi(num);

    // rmbBuy 表示直接发货，无需扣除消耗（订单服回调发货时为true，不再扣除消耗，直接发货）
    if (!rmbBuy) {
      ItemDropResult result =
          costDrop.deductItem(
              playerId,
              CurrencyAction.BUY_SHOP_CONSUME,
              DropParams.build().setGoodsType(goodsType).setGoodsId(goodsId).noNeedPop());

      // 非付费内消耗，扣除道具
      if (result.isError()) {
        PopGoods popGoods = result.getPopGoods();
        if (!popGoods.getPopGoodsIdList().isEmpty()) {
          // 构建弹出礼包信息
          for (int popGoodsId : popGoods.getPopGoodsIdList()) {
            ConfigShopDTO popShop = ShopConfigManager.getConfigShop(popGoodsId);
            resMsg.getPopGoodsList().add(popShop.buildGoodsBean(playerId));
          }
          resMsg.setFreezeItemNum(popGoods.getFreezeItemNum());
          resMsg.getPopGoodsReasonList().addAll(popGoods.getPopGoodsReasonList());
        }
        return result.getCode();
      }
    }

    // 更新购买数据
    PlayerBuyGoodsDO playerBuyGoodsDO =
        playerBuyGoodsDOOperator.getOrCreatePlayerBuyGoodsDO(playerId, goodsId);
    playerBuyGoodsDO.addCount(num);
    playerBuyGoodsDOOperator.update(playerId, playerBuyGoodsDO);

    // 购买指定商品数量事件
    ActionUtil.buyTrigger(playerId, goodsType, goodsId, num);
    // 商城埋点统计
    statisticShop(playerId, goodsId, num, goodsType, costDrop.getRewardStr(), rmbBuy);

    // 发货
    afterBuy(playerId, goodsId, num, configShop, resMsg);

    return ErrorCode.SUCCESS;
  }

  /**
   * 商城统计
   *
   * @param playerId player
   * @param goodsId goodsId
   * @param goodsType goodsType
   * @param cost cost
   * @param rmb rmb
   * @since 2025/3/7 14:35
   */
  private void statisticShop(
      long playerId, int goodsId, int num, int goodsType, String cost, boolean rmb) {
    PlayerBaseDO playerBaseDO = PlayerBaseDOOperator.self().getPlayerBaseDO(playerId);

    // 充值时，统计玩家日志
    StatisticManager.record(
        new PlayerShopStatisticPo()
            .setGoodsId(goodsId)
            .setGoodsType(goodsType)
            .setCost(cost)
            .setRmb(BooleanUtil.toByte(rmb))
            .setNum(num)
            .setPlayerId(playerId)
            .setRobot(PlayerUtil.getRobot(playerId))
            .setChannel(playerBaseDO.getChannel())
            .setLastChannel(playerBaseDO.getLastChannel())
            .setRegisterPlayer(PlayerUtil.getRegisterPlayer(playerId)));
  }

  /**
   * 购买成功后的处理逻辑 发货等
   *
   * @param playerId playerId
   * @param goodsId goodsId
   * @param num 购买数量
   * @param configShop configShop
   * @param resMsg resMsg
   * @since 2024/11/20 10:29
   */
  private void afterBuy(
      long playerId, int goodsId, int num, ConfigShopDTO configShop, WCBuyShopGoodsMessage resMsg) {
    DropParams dropParams =
        DropParams.build().setGoodsType(goodsType).setGoodsId(goodsId).setNum(num);
    // 奖励解析
    List<ItemBean> itemBeans =
        Drop.of(configShop.getRewards())
            .multi(num)
            .rewardItem(playerId, CurrencyAction.BUY_SHOP_REWARD, dropParams);
    // 额外奖励解析
    List<ItemBean> extItemBeans =
        Drop.of(configShop.getExtraRewards())
            .multi(num)
            .rewardItem(playerId, CurrencyAction.BUY_SHOP_EXT_REWARD, dropParams);

    resMsg.setGoodsId(goodsId);
    resMsg.getRewards().addAll(itemBeans);
    resMsg.getRewards().addAll(extItemBeans);

    // 购买之后逻辑
    handleAfterBuy(playerId, num, configShop, resMsg);

    // 刷新功能模版状态 todo  是否只在变动后才推送
    int functionId = ShopConfigManager.getFunctionId(goodsType);
    Template template = FunctionIdEnum.loadFuncTemplate(playerId, functionId);
    if (template != null) {
      template.pushFunctionStateMessage(playerId);
    }
    log.info(playerId, "busShopGoods", "success", "goodsId", goodsId, "rmbBuy");
  }

  /** 处理购买之后逻辑 */
  protected void handleAfterBuy(
      long playerId, int num, ConfigShopDTO configShop, WCBuyShopGoodsMessage resMsg) {}

  @Override
  public List<GoodsBean> buildGoodsBeanList(long playerId) {
    return ShopConfigManager.getConfigShopList(goodsType).stream()
        .map(conf -> buildGoodsBean(playerId, conf))
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public final GoodsBean buildGoodsBean(long playerId, int goodsId) {
    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);
    if (configShop == null) {
      return null;
    }
    if (configShop.getType() != goodsType) {
      return null;
    }

    return buildGoodsBean(playerId, configShop);
  }

  /**
   * 构建商品信息
   *
   * @param playerId playerId
   * @param configShop configShop
   * @return com.game.proto.shop.GoodsBean
   * @since 2024/11/20 11:23
   */
  protected GoodsBean buildGoodsBean(long playerId, ConfigShopDTO configShop) {
    return configShop.buildGoodsBean(playerId);
  }

  // @Override
  // public final ItemBean getPriceBean(ConfigShop configShop) {
  //   int goodsId = Integer.parseInt(configShop.getData_id());
  //
  //   String priceStr = configShop.getPrice();
  //   if (StringUtils.isBlank(priceStr)) {
  //     priceStr = CoreConstants.FREE_PRICE_DROP_STR;
  //   }
  //
  //   Drop priceDrop = Drop.of(priceStr);
  //   ItemBean itemBean = priceDrop.buildItemBeans().get(0);
  //   ConfigPayID configPayID = ShopConfigManager.getConfigPayID(goodsId);
  //   // 人民币支付
  //   if (checkRmbPrice(configShop) && configPayID != null) {
  //     // TODO 设置 rmb 价格
  //     int price = configPayID.getPrice();
  //     itemBean.setNum((long) price);
  //   }
  //
  //   return itemBean;
  // }
  //
  // private boolean checkRmbPrice(ConfigShop configShop) {
  //   return RewardEnum.CURRENCY.getNum(configShop.getPrice()) > 0;
  // }

  /** 获取商品类型关联的功能模板DO */
  protected PlayerTemplateDO getTemplateDO(long playerId) {
    int functionId = ShopConfigManager.getFunctionId(goodsType);
    return playerTemplateDOOprator.getOrNew(playerId, functionId);
  }
}
