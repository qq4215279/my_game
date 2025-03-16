package com.mumu.framework.core.rank;

import com.mumu.framework.core.autoinit.AutoInitEvent;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.util.SpringContextUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * RankManager
 * 排行榜管理器
 * @author liuzhen
 * @version 1.0.0 2025/3/15 16:31
 */
@Service
public class RankManager implements AutoInitEvent {

  public static RankManager self() {
    return SpringContextUtils.getBean(RankManager.class);
  }

  private static final LogTopic log = LogTopic.ACTION;

  /** 积分区间大小 */
  private static final int POINTS_INTERVAL = 10;
  /** 最大准确数量 */
  private static final int MAX_NUM = 100;


  // @Resource
  // private PlayerRankDOOperator playerRankDOOperator;

  /** rankMap */
  private final Map<RankEnum, Rank> RANK_MAP = new ConcurrentHashMap<>();

  @Override
  public void autoInit() {
    init();
  }

  /**
   * 初始化所有排行榜
   * @date 2025/3/15 16:55
   */
  private void init() {
    for (RankEnum rankEnum : RankEnum.values()) {
      initRank(rankEnum);
    }

  }

  /**
   * 初始化排行榜
   * @param rankEnum rankEnum
   * @date 2025/3/15 17:31
   */
  private void initRank(RankEnum rankEnum) {
    List<RankData> rankDataList = new ArrayList<>();

    int type = rankEnum.getType();
    // TODO
    /*for (PlayerRankDO playerRankDO : playerRankDOOperator.getPlayerRankDOList(type)) {
      rankDataList.add(RankData.of(playerRankDO));
    }*/

    Rank rank = new Rank(MAX_NUM, POINTS_INTERVAL);
    Collections.sort(rankDataList);
    rank.init(rankDataList);
    RANK_MAP.put(rankEnum, rank);

    log.info("initRank", "rankType", type);
  }


  /**
   * 获取排行榜
   * @param rankEnum rankEnum
   * @return com.game.framework.core.rank.Rank
   * @date 2025/3/15 17:32
   */
  private Rank getRank(RankEnum rankEnum) {
    return RANK_MAP.get(rankEnum);
  }

  /**
   * 增加对象
   * @param rankEnum rankEnum
   * @param data data
   * @date 2025/3/15 17:32
   */
  public void addValue(RankEnum rankEnum, RankData data) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return;
    }
    rank.addRankData(data);
  }

  /**
   * 更新积分
   * @param rankEnum rankEnum
   * @param data data
   * @param srcValue srcValue
   * @param srcTimestamp timestamp
   * @date 2025/3/15 17:32
   */
  public void updateValue(RankEnum rankEnum, RankData data, long srcValue, long srcTimestamp) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return;
    }

    rank.updateRankData(data, srcValue, srcTimestamp);
  }

  /**
   * 移除指定排行榜
   * @param rankEnum rankEnum
   * @date 2025/3/15 19:51
   */
  public void resetRank(RankEnum rankEnum) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return;
    }

    RANK_MAP.put(rankEnum, new Rank(rank.pointInterval, rank.maxNum));
  }

  /**
   * 移除对象
   * @param rankEnum rankEnum
   * @param data data
   * @date 2025/3/15 17:32
   */
  public void removeValue(RankEnum rankEnum, RankData data) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return;
    }
    rank.removeRankData(data);
  }

  /**
   * 获得自己的排名
   * @param rankEnum rankEnum
   * @param data data
   * @return int
   * @date 2025/3/15 17:32
   */
  public int getMyRank(RankEnum rankEnum, RankData data) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return -1;
    }

    return rank.getMyRank(data);
  }

  /**
   * 获取排名
   * @param rankEnum rankEnum
   * @param start start
   * @param end end
   * @return java.util.List<com.game.framework.core.rank.RankData>
   * @date 2025/3/15 17:32
   */
  public List<RankData> getRankList(RankEnum rankEnum, int start, int end) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return Collections.emptyList();
    }

    return rank.getRankList(start, end);
  }

  /**
   * 获取排名
   * @param rankEnum rankEnum
   * @return java.util.List<com.game.framework.core.rank.RankData>
   * @date 2025/3/15 17:32
   */
  public List<RankData> getRankList(RankEnum rankEnum) {
    int end = getMaxNum(rankEnum);
    if (end == 0) {
      return Collections.emptyList();
    }
    return getRankList(rankEnum, 0, end);
  }

  /**
   * 获取排名
   * @param rankEnum rankEnum
   * @return int
   * @date 2025/3/15 17:32
   */
  public int getMaxNum(RankEnum rankEnum) {
    Rank rank = getRank(rankEnum);
    if (null == rank) {
      return 0;
    }
    return rank.getMaxNum();
  }

}
