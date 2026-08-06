package com.mumu.game.business.shop.operator;

import com.game.business.shop.domain.PlayerPopFaceGoodsDO;
import com.game.framework.core.automodel.entity.IDataOperator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * PlayerPopFaceGoodsDOOperator
 * @author liuzhen
 * @version 1.0.0 2025/7/8 16:38
 */
@Component
public class PlayerPopFaceGoodsDOOperator implements IDataOperator<PlayerPopFaceGoodsDO> {

  /**
   * getPlayerPopFaceGoodsDOList
   * @param playerId playerId
   * @return java.util.List<com.game.business.shop.domain.PlayerPopFaceGoodsDO>
   * @since 2025/7/8 17:15
   */
  public List<PlayerPopFaceGoodsDO> getPlayerPopFaceGoodsDOList(long playerId) {
    return selectAllReverse(playerId);
  }

  /**
   * getPlayerPopFaceGoodsDO
   * @param playerId playerId
   * @param goodsId goodsId
   * @return com.game.business.shop.domain.PlayerPopFaceGoodsDO
   * @since 2025/7/8 17:15
   */
  public PlayerPopFaceGoodsDO getPlayerPopFaceGoodsDO(long playerId, int goodsId) {
    return selectOne(playerId, playerId, goodsId);
  }

  /**
   * 创建
   * @param playerId playerId
   * @param goodsId goodsId
   * @return com.game.business.shop.domain.PlayerPopFaceGoodsDO
   * @since 2025/7/8 17:21
   */
  public void createPlayerPopFaceGoodsDO(long playerId, int goodsId, int taskId) {
    PlayerPopFaceGoodsDO playerPopFaceGoodsDO = new PlayerPopFaceGoodsDO();
    playerPopFaceGoodsDO.setPlayerId(playerId);
    playerPopFaceGoodsDO.setGoodsId(goodsId);
    playerPopFaceGoodsDO.setPopFaceTime(System.currentTimeMillis());
    playerPopFaceGoodsDO.setTriggerTaskId(taskId);
    // 弹出红点
    playerPopFaceGoodsDO.setHasPoint(true);
    insertOrUpdate(playerId, playerPopFaceGoodsDO);
  }

  /**
   * 删除弹脸礼包
   * @param playerId playerId
   * @param goodsId goodsId
   * @since 2025/7/10 10:35
   */
  public void deletePlayerPopFaceGoodsDO(long playerId, int goodsId) {
    deleteOne(playerId, playerId, goodsId);
  }
}
