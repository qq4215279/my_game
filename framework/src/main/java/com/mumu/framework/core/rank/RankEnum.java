package com.mumu.framework.core.rank;

import com.game.business.rank.domain.PlayerRankDO;
import com.game.business.rank.operator.PlayerRankDOOperator;
import com.game.template.func.enums.ResetEnum;
import java.util.List;
import lombok.Getter;

/**
 * RankEnum
 * 排行榜枚举
 * @author liuzhen
 * @version 1.0.0 2025/3/15 17:06
 */
@Getter
public enum RankEnum {
  /** bet小游戏近20局金币榜 */
  BET_GOLD(1),
  ;

  /** 排行榜类型 */
  private final int type;
  /** 重置类型(默认 终生不重置) */
  private ResetEnum resetEnum = ResetEnum.INFINITE;

  RankEnum(int type) {
    this.type = type;
  }

  RankEnum(int type, ResetEnum resetEnum) {
    this.type = type;
    this.resetEnum = resetEnum;
  }


  /**
   * 更新排行榜
   * @param playerId playerId
   * @param score score
   * @date 2025/3/15 19:34
   */
  public void updateRank(long playerId, int score) {
    updateRank(playerId, score, 0);
  }

  /**
   * 更新排行榜
   * @param playerId playerId
   * @param score score
   * @param score2 score2
   * @date 2025/3/15 19:34
   */
  public void updateRank(long playerId, int score, int score2) {
    PlayerRankDOOperator playerRankDOOperator = PlayerRankDOOperator.self();
    RankManager rankManager = RankManager.self();

    boolean add = false;
    PlayerRankDO playerRankDO = playerRankDOOperator.getPlayerRankDO(type, playerId);
    if (playerRankDO != null) {
      playerRankDO = playerRankDOOperator.insertPlayerRankDO(type, playerId);
      add = true;
    }

    int srcScore = playerRankDO.getScore();
    long srcTimestamp = playerRankDO.getTimestamp();

    playerRankDO.setScore(srcScore + score);
    playerRankDO.setScore2(score2);
    playerRankDO.setTimestamp(System.currentTimeMillis());
    playerRankDOOperator.update(playerRankDO);

    if (add) {
      rankManager.addValue(this, getRankData(playerRankDO));
    } else {
      rankManager.updateValue(this, getRankData(playerRankDO), srcScore, srcTimestamp);
    }

  }

  /**
   * 重置排行榜
   * @date 2025/3/15 19:50
   */
  public void resetRank() {
    // 清除数据
    PlayerRankDOOperator.self().deleteAll(type);
    // 重置排行榜
    RankManager.self().resetRank(this);
  }

  /**
   * 获取自己当前排名
   * @param playerId playerId
   * @return int
   * @date 2025/3/15 19:44
   */
  public int getMyRank(long playerId) {
    PlayerRankDOOperator playerRankDOOperator = PlayerRankDOOperator.self();
    RankManager rankManager = RankManager.self();

    PlayerRankDO playerRankDO = playerRankDOOperator.getPlayerRankDO(type, playerId);
    if (playerRankDO == null) {
      return -1;
    }

    return rankManager.getMyRank(this, getRankData(playerRankDO));
  }

  /**
   * 获取排行榜列表
   * @param start start
   * @param end end
   * @return java.util.List<com.game.framework.core.rank.RankData>
   * @date 2025/3/15 19:45
   */
  public List<RankData> getRankList(int start, int end) {
    RankManager rankManager = RankManager.self();
    return rankManager.getRankList(this, start, end);
  }

  /**
   * 获取排行榜列表
   * @return java.util.List<com.game.framework.core.rank.RankData>
   * @date 2025/3/15 19:45
   */
  public List<RankData> getRankList() {
    RankManager rankManager = RankManager.self();
    return rankManager.getRankList(this);
  }

  /**
   * 创建RankData
   * @param playerRankDO playerRankDO
   * @return com.game.framework.core.rank.RankData
   * @date 2025/3/15 19:45
   */
  private RankData getRankData(PlayerRankDO playerRankDO) {
   return RankData.of(playerRankDO);
  }

}
