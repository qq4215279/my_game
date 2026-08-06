package com.mumu.game.template.shop.core.template;

import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.shop.GoodsBean;
import com.mumu.game.proto.shop.WCBuyShopGoodsMessage;

import java.util.List;


/**
 * ShopTemp
 * 商品模版
 * @author liuzhen
 * @version 1.0.0 2024/11/19 19:39
 */
public interface ShopTemp {

  /**
   * 校验礼包红点
   * @param playerId playerId 
   * @return boolean
   * @since 2024/12/19 10:20
   */
  boolean checkRedPoint(long playerId);

  /**
   * 检查是否可以购买
   * @param goodsId 商品id
   * @param num 商品购买数量
   * @return cn.hutool.core.lang.Pair<java.lang.Boolean,com.game.proto.core.ErrorCode>
   * @since 2024/11/19 19:38
   */
  ErrorCode checkBuyGoods(long playerId, int goodsId, int num);

  /**
   * 购买商品
   * @param playerId playerId
   * @param goodsId goodsId
   * @param num num
   * @param rmbBuy rmbBuy
   * @param resMsg resMsg
   * @return boolean
   * @since 2024/11/20 10:21
   */
  ErrorCode busShopGoods(long playerId, int goodsId, int num, boolean rmbBuy, WCBuyShopGoodsMessage resMsg);

  /**
   * 构建商品bean列表
   * @param playerId playerId
   * @return java.util.List<com.game.proto.shop.GoodsBean>
   * @since 2024/11/20 10:44
   */
  List<GoodsBean> buildGoodsBeanList(long playerId);

  /**
   * 获取商店商品
   * @param goodsId 商品id
   * @return com.game.proto.shop.GoodsBean 商品信息
   * @since 2024/11/19 19:39
   */
  GoodsBean buildGoodsBean(long playerId, int goodsId);

  // /**
  //  * 获取商品价格bean
  //  * @param configShop configShop
  //  * @return com.game.proto.item.ItemBean
  //  * @since 2024/10/10 15:10
  //  */
  // ItemBean getPriceBean(ConfigShop configShop);

}
