package com.mumu.game.shop.service;

import com.mumu.game.business.shop.luban.ShopConfigManager;
import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.drop.utils.ItemUtil;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.shop.GoodsBean;
import com.mumu.game.proto.shop.WCBuyShopGoodsMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsByGoodsIdMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsByTypeMessage;
import com.mumu.game.proto.shop.WCGetShopGoodsMessage;
import com.mumu.game.template.func.core.temp.Template;
import org.springframework.stereotype.Service;

/**
 * ShopServiceImpl 商城service
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/25 15:18
 */
@Service
public class ShopServiceImpl implements ShopService {
  private static final LogTopic log = LogTopic.ACTION;

  @Override
  public ResponseResult getShopGoodsInfoList(long playerId, int functionId) {
    // 功能开发判断
    /*Template template = FunctionIdEnum.loadFuncTemplate(playerId, functionId);
    if (template == null) {
      return ResponseResult.errorByParam(playerId);
    }
    if (!template.isOpen(playerId)) {
      return ResponseResult.errorByNotOpen(playerId);
    }*/

    WCGetShopGoodsMessage resMsg = new WCGetShopGoodsMessage();
    /*resMsg.setFunctionId(functionId);
    for (ShopTemp shopTemp : ShopTemplateManager.getShopTemplateList(functionId)) {
      resMsg.getGoodsList().addAll(shopTemp.buildGoodsBeanList(playerId));
    }*/

    return ResponseResult.success(playerId, resMsg);
  }

  @Override
  public ResponseResult getShopGoodsInfoListByType(long playerId, int type) {
    /*ShopTemp shopTemp = ShopTemplateManager.getShopTemplate(type);
    if (shopTemp == null) {
      return ResponseResult.errorByParam(playerId);
    }

    int functionId = ShopConfigManager.getFunctionId(type);
    // 功能开发判断
    Template template = FunctionIdEnum.loadFuncTemplate(playerId, functionId);
    if (template == null) {
      return ResponseResult.errorByParam(playerId);
    }
    if (!template.isOpen(playerId)) {
      return ResponseResult.errorByNotOpen(playerId);
    }*/

    WCGetShopGoodsByTypeMessage resMsg = new WCGetShopGoodsByTypeMessage();
    // resMsg.setFunctionId(functionId);
    // resMsg.getGoodsList().addAll(shopTemp.buildGoodsBeanList(playerId));

    // 返回
    return ResponseResult.success(playerId, resMsg);
  }

  @Override
  public ResponseResult getShopGoodsInfoByGoodsId(long playerId, int goodsId) {
   /* ShopTemp shopTemp = ShopTemplateManager.getShopTemplateByGoodsId(goodsId);
    if (shopTemp == null) {
      return ResponseResult.error(playerId, ErrorCode.FAIL_GOODS_NOT_EXIST);
    }

    ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);
    if (configShop == null) {
      return ResponseResult.errorByParam(playerId);
    }

    // 功能开发判断
    int functionId = ShopConfigManager.getFunctionId(configShop.getType());
    Template template = FunctionIdEnum.loadFuncTemplate(playerId, functionId);
    if (template == null) {
      return ResponseResult.errorByParam(playerId);
    }
    if (!template.isOpen(playerId)) {
      return ResponseResult.errorByNotOpen(playerId);
    }*/

    WCGetShopGoodsByGoodsIdMessage resMsg = new WCGetShopGoodsByGoodsIdMessage();
    // resMsg.setGoodsBean(shopTemp.buildGoodsBean(playerId, goodsId));
    return ResponseResult.success(playerId, resMsg);
  }

  @Override
  public GoodsBean buildGoodsBean(long playerId, int goodsId) {
    // ShopTemp shopTemp = ShopTemplateManager.getShopTemplateByGoodsId(goodsId);
    // if (shopTemp == null) {
    //   return null;
    // }
    // return shopTemp.buildGoodsBean(playerId, goodsId);
    return null;
  }

  @Override
  public ErrorCode checkBuyGoods(long playerId, int num, ConfigShopDTO configShop) {
    // 商品id
    /*int goodsId = configShop.getGoodsId();
    ShopTemp shopTemp = ShopTemplateManager.getShopTemplateByGoodsId(goodsId);
    if (shopTemp == null) {
      return ErrorCode.FAIL_GOODS_NOT_EXIST;
    }

    return shopTemp.checkBuyGoods(playerId, goodsId, num);*/
    return ErrorCode.SUCCESS;
  }

  @Override
  public ErrorCode busShopGoods(long playerId, int goodsId, int num, boolean rmbBuy) {
    // ConfigShopDTO configShop = ShopConfigManager.getConfigShop(goodsId);
    // if (configShop == null) {
    //   MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_GOODS_NOT_EXIST);
    //   return ErrorCode.FAIL_GOODS_NOT_EXIST;
    // }
    //
    // ShopTemp shopTemp = ShopTemplateManager.getShopTemplateByGoodsId(goodsId);
    // if (shopTemp == null) {
    //   MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_PARAM_ERROR);
    //   return ErrorCode.FAIL_PARAM_ERROR;
    // }
    //
    // // 人民币购买不校验，只要可以创建订单，充值成功，则直接发货！
    // if (!rmbBuy) {
    //   ErrorCode checkBuyErrorCode = shopTemp.checkBuyGoods(playerId, goodsId, num);
    //   // 购买上限
    //   if (checkBuyErrorCode != ErrorCode.SUCCESS) {
    //     MessageSender.sendToPlayer(playerId, checkBuyErrorCode);
    //     return checkBuyErrorCode;
    //   }
    // }
    //
    // // 构建奖励返回
    // WCBuyShopGoodsMessage resMsg = new WCBuyShopGoodsMessage();
    // ErrorCode errorCode = shopTemp.busShopGoods(playerId, goodsId, num, rmbBuy, resMsg);
    //
    // // 合并奖励
    // resMsg.setRewards(ItemUtil.mergeRewards(resMsg.getRewards()));
    // MessageSender.sendToPlayer(playerId, errorCode, Cmd.WCBuyShopGoods, resMsg);

    // return errorCode;
    return ErrorCode.SUCCESS;
  }




  // @Override
  // public ItemBean getPriceBean(ConfigShopDTO configShop) {
  //   ShopTemp shopTemp = ShopTemplateManager.getShopTemplate(configShop.getType());
  //   return shopTemp.getPriceBean(configShop);
  // }

  // @Override
  // public ResponseResult getGoldReturn(long playerId) {
  //   GoldReturnGoodsTemplate template =
  //       FunctionIdEnum.SHOP_GOODS_GOLD_RETURN.loadFuncTemplate(playerId);
  //   if (template == null || !template.isOpen(playerId)) {
  //     return ResponseResult.errorByNotOpen(playerId);
  //   }
  //
  //   // if (!FunctionIdEnum.SHOP_GOODS.isOpen(playerId)) {
  //   //   return ResponseResult.errorByNotOpen(playerId);
  //   // }
  //
  //   WCGetGoldReturnMessage resMsg = new WCGetGoldReturnMessage();
  //   resMsg.setBean(template.buildGoldReturnBean(playerId));
  //
  //   // 1.填充商城礼包页签数据
  //   // for (ShopTemp shopTemp :
  //   //     ShopTemplateManager.getShopTemplateList(FunctionIdEnum.SHOP_GOODS.getFunctionId())) {
  //   //   resMsg.getGoodsList().addAll(shopTemp.buildGoodsBeanList(playerId));
  //   // }
  //   // 2.填充金币回购礼包信息
  //   // GoldReturnGoodsTemplate goldTemp =
  //   //     FunctionIdEnum.SHOP_GOODS_GOLD_RETURN.loadFuncTemplate(playerId);
  //   // if (goldTemp != null && goldTemp.isOpen(playerId)) {
  //   //   resMsg.setBean(goldTemp.buildGoldReturnBean(playerId));
  //   // }
  //   return ResponseResult.success(playerId, Cmd.WCGetGoldReturn, resMsg);
  // }
  //
  // @Override
  // public ResponseResult buyGoldReturn(long playerId) {
  //   GoldReturnGoodsTemplate template =
  //       FunctionIdEnum.SHOP_GOODS_GOLD_RETURN.loadFuncTemplate(playerId);
  //   if (template == null || !template.isOpen(playerId)) {
  //     return ResponseResult.errorByNotOpen(playerId);
  //   }
  //
  //   Pair<ErrorCode, ItemBean> pair = template.buyGoldReturn(playerId);
  //   if (pair.getKey() != ErrorCode.SUCCESS) {
  //     return ResponseResult.error(playerId, pair.getKey());
  //   }
  //
  //   WCBuyGoldReturnMessage resMsg = new WCBuyGoldReturnMessage();
  //   resMsg.setReward(pair.getValue());
  //   return ResponseResult.success(playerId, Cmd.WCBuyGoldReturn, resMsg);
  // }
}
