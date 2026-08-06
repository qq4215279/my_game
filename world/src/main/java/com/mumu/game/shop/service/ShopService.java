package com.mumu.game.shop.service;


import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.shop.GoodsBean;

/**
 * ShopService
 * 商城service
 * @author liuzhen
 * @version 1.0.0 2024/9/25 15:18
 */
public interface ShopService {

  /**
   * 请求商品信息列表
   * @param playerId 玩家id
   * @param functionId 功能id
   * @since 2024/9/25 15:28
   */
  ResponseResult getShopGoodsInfoList(long playerId, int functionId);

  /**
   * 请求商品信息列表By商品类型
   * @param playerId 玩家id
   * @param type 商品类型
   * @since 2024/10/22 15:06
   */
  ResponseResult getShopGoodsInfoListByType(long playerId, int type);

  /**
   * 请求商品信息By商品id
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @since 2024/10/22 15:06
   */
  ResponseResult getShopGoodsInfoByGoodsId(long playerId, int goodsId);

  /**
   * 构建商品信息bean
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @return com.game.proto.shop.GoodsBean
   * @since 2024/9/25 15:53
   */
  GoodsBean buildGoodsBean(long playerId, int goodsId);

  /**
   * 校验能否购买商品
   * @param playerId playerId
   * @param num 购买数量
   * @param configShop configShop
   * @return boolean
   * @since 2024/10/9 16:06
   */
  ErrorCode checkBuyGoods(long playerId, int num, ConfigShopDTO configShop);

  /**
   * 请求购买商品
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @param num 商品购买数量
   * @param rmbBuy 人民币购买
   * @since 2024/9/25 15:28
   */
  ErrorCode busShopGoods(long playerId, int goodsId, int num, boolean rmbBuy);

  // /**
  //  * 获取商品价格bean
  //  * @param configShop configShop
  //  * @return com.game.proto.item.ItemBean
  //  * @since 2024/10/10 15:10
  //  */
  // ItemBean getPriceBean(ConfigShopDTO configShop);

  // /** 请求商城礼包列表 */
  // ResponseResult getGoldReturn(long playerId);


  // /** 购买高额金币损失回购 */
  // ResponseResult buyGoldReturn(long playerId);

}
