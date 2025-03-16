package com.mumu.framework.core.rank;

import com.game.business.rank.domain.PlayerRankDO;

/**
 * RankData
 * 排行榜数据
 * @author liuzhen
 * @version 1.0.0 2025/3/15 16:30
 */
public class RankData implements Comparable<RankData> {
  /** 虚拟主键（默认是玩家id）  */
  protected long vid;
  /** 排行主体标识  */
  protected long id;
  /** rank分 */
  protected long score;
  /** rank分2 */
  protected long score2;
  /** 排行时间 */
  protected long timestamp;
  /** TODO 额外参数 */
  protected int[] params;

  /** 空对象 */
  static final RankData empty = new RankData();


  public static RankData createEmpty() {
    return empty;
  }

  // TODO
  // public static RankData of(PlayerRankDO playerRankDO) {
  //   RankData data = new RankData();
  //   data.vid = playerRankDO.getPlayerId();
  //   data.id = playerRankDO.getPlayerId();
  //   data.score = playerRankDO.getScore();
  //   data.score2 = playerRankDO.getScore2();
  //   data.timestamp = playerRankDO.getTimestamp();
  //   return data;
  // }

  @Override
  public boolean equals(Object obj) {
    if (null == obj) {
      return false;
    }
    if (!(obj instanceof RankData o)) {
      return false;
    }

    return this.vid == o.vid;
  }

  @Override
  public int compareTo(RankData o) {
    // 1. 分支越大，排名靠前
    if (this.score > o.score) {
      return -1;
    } else if (this.score < o.score) {
      return 1;

      // 2. 分支越大，排名靠前
    } else if (this.score2 > o.score2) {
      return -1;
    } else if (this.score2 < o.score2) {
      return 1;

      // 3. 跟新时间靠后，排名靠前
    } else if (this.timestamp > o.timestamp) {
      return 1;
    } else if (this.timestamp < o.timestamp) {
      return -1;

      // 4. vid越小，排名靠前
    } else if (this.vid < o.vid) {
      return -1;
    } else if (this.vid > o.vid) {
      return 1;

    } else {
      return 0;
    }
  }

  @Override
  public RankData clone() {
    RankData data = new RankData();

    data.vid = this.vid;
    data.id = this.id;
    data.score = this.score;
    data.score2 = this.score2;
    data.timestamp = this.timestamp;
    data.params = this.params;

    return data;
  }

}
