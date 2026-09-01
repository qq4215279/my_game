package com.mumu.game.rank.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.game.business.player.event.world.OnlineEvent;
import com.game.business.player.operator.PlayerBaseDOOperator;
import com.game.business.rank.constants.RankConstants;
import com.game.business.rank.enums.RankFunc;
import com.game.business.rank.enums.RankType;
import com.game.business.rank.util.RankUtil;
import com.game.business.scroll.ScrollTmpKey;
import com.game.collection.LRULinkedHashMap;
import com.game.framework.core.autoinit.AutoInitEvent;
import com.game.framework.core.autotimer.anno.AutoScheduled;
import com.game.framework.core.autotimer.consts.AutoScheduleKey;
import com.game.framework.core.autotimer.consts.Cron;
import com.game.proto.rank.PlayerRankBean;
import com.google.common.collect.Maps;

import jakarta.annotation.Resource;

/** 排行榜服务 @Date: 2024/11/29 下午4:19 @Author: xu.hai */
@Service
public class RankService implements AutoInitEvent {

  @Resource PlayerBaseDOOperator playerBaseDOOperator;

  /** 版本号 */
  final AtomicInteger version = new AtomicInteger();

  /** k1-功能榜单，k2-榜单类型，k3-周期id，val-榜单列表 */
  final Map<RankFunc, Map<RankType, Map<Integer, Map<Long, PlayerRankBean>>>> rankInfos =
      Maps.newConcurrentMap();

  @Override
  public void autoInit() {
    refreshRank(true, RankConstants.FUNC_REFRESH_ALL);
  }

  /** 排行榜前3的玩家上线有跑马灯 */
  @EventListener(OnlineEvent.class)
  public void handlerScrollEvent(OnlineEvent event) {
    long playerId = event.getPlayerId();
    boolean isTop3 =
        RankConstants.FUNC_ONLINE_SCROLL.stream()
            .map(rankInfos::get)
            .filter(Objects::nonNull)
            .flatMap(map -> map.values().stream())
            .flatMap(map -> map.values().stream())
            .map(map -> map.get(playerId))
            .filter(Objects::nonNull)
            .anyMatch(rank -> rank.getRank() <= 3);

    if (isTop3) ScrollTmpKey.WELCOME.send(playerBaseDOOperator.getNick(playerId));
  }

  @AutoScheduled(key = AutoScheduleKey.RANK_REFRESH_GOLD, cron = Cron.EVERY_30_SECOND)
  public void runTaskGold() {
    refreshRank(false, RankConstants.FUNC_REFRESH_30S);
  }

  @AutoScheduled(key = AutoScheduleKey.RANK_REFRESH, cron = Cron.EVERY_5_MINUTE)
  public void runTask() {
    refreshRank(false, RankConstants.FUNC_REFRESH_5MIN);
  }

  private void refreshRank(boolean isInit, Collection<RankFunc> refreshFuncs) {
    for (RankFunc rankFunc : refreshFuncs) {
      var infoMap = isInit ? rankFunc.getAllTopNMap() : rankFunc.getCurrTopNMap();
      infoMap.forEach(
          (typeInfo, map) ->
              map.forEach(
                  (periodId, scores) -> {
                    Map<Long, PlayerRankBean> rankBeans =
                        RankUtil.createPlayerRanks(scores, typeInfo.getRankLimit());
                    rankInfos
                        .computeIfAbsent(rankFunc, k -> Maps.newConcurrentMap())
                        .computeIfAbsent(
                            typeInfo.getType(), k -> LRULinkedHashMap.of(typeInfo.getPeriodNum()))
                        .put(periodId, rankBeans);
                  }));
    }
    // 版本号递增
    version.incrementAndGet();
  }

  /** 获取版本号 */
  public int getVersion() {
    return version.get();
  }

  /** 获取指定玩家的排行榜信息 */
  public PlayerRankBean getRankInfo(long playerId, RankFunc func, RankType type, int periodOffset) {
    int periodId = type.getPeriodIdByOffset(periodOffset);
    PlayerRankBean rankBean = getRankInfos(func, type, periodId).get(playerId);
    return rankBean != null
        ? rankBean
        : RankUtil.createPlayerRank(
            func.getRedisRank().getScoreInfo(playerId, type, periodId),
            func.getType(type).getRankLimit());
  }

  /**
   * 获取指定榜单信息（分页）
   *
   * @param func 榜单功能
   * @param type 榜单类型
   * @param periodOffset 周期id偏移量
   * @param rankOffset 排名偏移量
   * @return 排行榜列表
   */
  public List<PlayerRankBean> getRankInfos(
      RankFunc func, RankType type, int periodOffset, int rankOffset) {
    return getRankInfos(func, type, type.getPeriodIdByOffset(periodOffset)).values().stream()
        .filter(rank -> rank.getRank() > rankOffset)
        .limit(RankConstants.PAGE_SIZE)
        .collect(Collectors.toList());
  }

  /**
   * 获取榜单信息
   *
   * @param func 榜单功能
   * @param type 榜单类型
   * @param periodId 周期id
   * @return 榜单列表
   */
  private Map<Long, PlayerRankBean> getRankInfos(RankFunc func, RankType type, int periodId) {
    return Optional.ofNullable(rankInfos.get(func))
        .map(map -> map.get(type))
        .map(map -> map.get(periodId))
        .orElseGet(Collections::emptyMap);
  }
}
