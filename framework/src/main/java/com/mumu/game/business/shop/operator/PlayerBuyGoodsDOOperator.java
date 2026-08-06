package com.mumu.game.business.shop.operator;

import java.util.List;

import com.mumu.game.business.shop.domain.PlayerBuyGoodsDO;
import com.mumu.game.business.shop.luban.ShopConfigManager;
import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.template.func.enums.ResetEnum;
import org.springframework.stereotype.Component;

/**
 * PlayerBuyGoodsDOOperator
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/26 13:53
 */
@Component
public class PlayerBuyGoodsDOOperator  {

  public static PlayerBuyGoodsDOOperator self() {
    return SpringContextUtils.getBean(PlayerBuyGoodsDOOperator.class);
  }

  /** 获取商品购买次数 */
  public int getCount(long playerId, int goodsId) {
    PlayerBuyGoodsDO goodsDO = getPlayerBuyGoodsDO(playerId, goodsId);
    return goodsDO != null ? goodsDO.getCount() : 0;
  }

  /**
   * 获取玩家购买信息
   *
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @return com.game.business.shop.domain.PlayerBuyGoodsDO
   * @since 2024/9/26 14:29
   */
  public PlayerBuyGoodsDO getPlayerBuyGoodsDO(long playerId, int goodsId) {
    // return selectOne(playerId, playerId, goodsId);
    return null;
  }

  public PlayerBuyGoodsDO getOrCreatePlayerBuyGoodsDO(long playerId, int goodsId) {
    PlayerBuyGoodsDO playerBuyGoodsDO = getPlayerBuyGoodsDO(playerId, goodsId);
    if (playerBuyGoodsDO == null) {
      playerBuyGoodsDO = createPlayerBuyGoodsDO(playerId, goodsId);
    }

    return playerBuyGoodsDO;
  }

  /**
   * 创建PlayerBuyGoodsDO
   *
   * @param playerId 玩家id
   * @param goodsId 商品id
   * @return com.game.business.shop.domain.PlayerBuyGoodsDO
   * @since 2024/9/26 14:02
   */
  private PlayerBuyGoodsDO createPlayerBuyGoodsDO(long playerId, int goodsId) {
    PlayerBuyGoodsDO playerBuyGoodsDO = new PlayerBuyGoodsDO();
    playerBuyGoodsDO.setPlayerId(playerId);
    playerBuyGoodsDO.setGoodsId(goodsId);
    playerBuyGoodsDO.setCount(0);
    playerBuyGoodsDO.setTotalCount(0);

    long now = System.currentTimeMillis();
    playerBuyGoodsDO.setLastResetTime(now);
    playerBuyGoodsDO.setFirstBuyTime(now);

    // TODO
    // insert(playerBuyGoodsDO);
    return playerBuyGoodsDO;
  }

  /**
   * 重置购买记录
   *
   * @param playerId playerId
   * @param functionId functionId
   * @since 2024/12/20 15:24
   */
  public void reset(long playerId, int functionId, ResetEnum resetEnum) {
    List<Integer> goodsTypes = ShopConfigManager.getGoodsTypeListByFuncId(functionId);
    for (int goodsType : goodsTypes) {
      try {
        for (ConfigShopDTO conf : ShopConfigManager.getConfigShopList(goodsType)) {
          // 限购类型
          if (conf.getResetType() != resetEnum) {
            continue;
          }
          reset(playerId, conf.getGoodsId());
        }

      } catch (Exception e) {
        LogTopic.ACTION.error(e, "shopReset", "playerId", playerId, "functionId", functionId);
      }
    }
  }

  /** 重置礼包 */
  public void reset(long playerId, int goodsId) {
    PlayerBuyGoodsDO goodsDO = getPlayerBuyGoodsDO(playerId, goodsId);
    if (goodsDO != null) {
      goodsDO.reset();
      // update(goodsDO);
      // TODO
    }
  }
}
