package com.mumu.game.shop.action;


import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.cmd.anno.CmdMapping;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.proto.shop.CWBuyShopGoodsMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsByGoodsIdMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsByTypeMessage;
import com.mumu.game.proto.shop.CWGetShopGoodsMessage;
import com.mumu.game.shop.service.ShopService;
import jakarta.annotation.Resource;

/**
 * ShopAction
 * 商城action
 * @author liuzhen
 * @version 1.0.0 2024/9/25 15:18
 */
@CmdAction
public class ShopAction {

  @Resource
  private ShopService shopService;

  /**
   * 请求商品信息列表
   * @param context context
   * @since 2024/9/25 15:25
   */
  @CmdMapping(Cmd.CWGetShopGoods)
  public ResponseResult getShopGoodsInfoList(MessageContext context) {
    CWGetShopGoodsMessage msg = context.getMsg(CWGetShopGoodsMessage.class);

    return shopService.getShopGoodsInfoList(context.getPlayerId(), msg.getFunctionId());
  }

  /**
   * 请求商品信息列表By商品类型
   * @param context context
   * @since 2024/10/22 15:04
   */
  @CmdMapping(Cmd.CWGetShopGoodsByType)
  public ResponseResult getShopGoodsInfoListByType(MessageContext context) {
    CWGetShopGoodsByTypeMessage msg = context.getMsg(CWGetShopGoodsByTypeMessage.class);

    return shopService.getShopGoodsInfoListByType(context.getPlayerId(), msg.getType());
  }

  /**
   * 请求商品信息By商品id
   * @param context context
   * @since 2024/10/22 15:05
   */
  @CmdMapping(Cmd.CWGetShopGoodsByGoodsId)
  public ResponseResult getShopGoodsInfoByGoodsId(MessageContext context) {
    CWGetShopGoodsByGoodsIdMessage msg = context.getMsg(CWGetShopGoodsByGoodsIdMessage.class);

    return shopService.getShopGoodsInfoByGoodsId(context.getPlayerId(), msg.getGoodsId());
  }

  /**
   * 请求购买商品
   * @param context context
   * @since 2024/9/25 15:25
   */
  @CmdMapping(Cmd.CWBuyShopGoods)
  public void busShopGoods(MessageContext context) {
    CWBuyShopGoodsMessage msg = context.getMsg(CWBuyShopGoodsMessage.class);
    shopService.busShopGoods(context.getPlayerId(), msg.getGoodsId(), 1, false);
  }

  // /**
  //  * 请求发送推送弹窗礼包
  //  * @param context context
  //  * @since 2024/9/25 15:25
  //  */
  // @CmdMapping(Cmd.AWSendOnPushPopGoods)
  // public void sendOnPushPopGoods(MessageContext context) {
  //   AWSendOnPushPopGoodsMessage msg = context.getMsg(AWSendOnPushPopGoodsMessage.class);
  //   ShopUtil.sendOnPushPopGoods(msg.getTargetPlayerId(), msg.getGoodsIdList(), msg.getFreezeItemNum(), msg.getPopGoodsReasonList());
  // }

  // @CmdMapping(Cmd.CWGetGoldReturn)
  // public ResponseResult getGoldReturn(MessageContext context) {
  //   return shopService.getGoldReturn(context.getPlayerId());
  // }

  // @CmdMapping(Cmd.CWBuyGoldReturn)
  // public ResponseResult buyGoldReturn(MessageContext context) {
  //   return shopService.buyGoldReturn(context.getPlayerId());
  // }

}
