package com.mumu.game.rank.luban;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.game.business.activity.luban.dto.RankConfigDTO;
import com.game.business.rank.enums.RankType;
import com.game.framework.core.auto.AutoInitLubanEvent;
import com.game.luban.activity.component.ChildrenConfigActivityProgressRewardProgressList;
import com.game.luban.activity.component.ComponentConfigLoader;
import com.game.luban.activity.component.ConfigActivityProgressReward;
import com.game.luban.activity.component.ConfigActivityRank;
import com.game.luban.activity.component.ConfigActivityTurntableDraw;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.Getter;

/**
 * ActivityConfigManager
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/15 20:45
 */
@Getter
@Component
public class ActivityConfigManager implements AutoInitLubanEvent<ComponentConfigLoader> {

  /** 活跃度进度奖励配置 */
  static Map<Integer, ConfigActivityProgressReward> progressRewardMap = Collections.emptyMap();

  /** 功能id 与 转盘抽奖活动 */
  static Map<Integer, ConfigActivityTurntableDraw> funcIdTurntableDrawMap = Collections.emptyMap();

  /** 功能id 与 排行榜活动 */
  @Getter
  static Table<Integer, String, RankConfigDTO> funcIdRankTypeRankMap = HashBasedTable.create();

  @Override
  public void autoLoad() {
    loadProgressReward();
    loadTurntableDrawReward();
    loadRankReward();
  }

  /** 加载活跃度进度奖励数据 */
  private void loadProgressReward() {
    Map<String, ConfigActivityProgressReward> map =
        getLubanLoader().getConfigActivityProgressRewardMap();
    if (CollUtil.isEmpty(map)) return;

    progressRewardMap = CollStreamUtil.toIdentityMap(map.values(), ConfigActivityProgressReward::getFunctionId);


  }

  /** 加载转盘抽奖活动数据 */
  private void loadTurntableDrawReward() {
    Map<String, ConfigActivityTurntableDraw> map =
        getLubanLoader().getConfigActivityTurntableDrawMap();
    if (CollUtil.isEmpty(map)) return;

    funcIdTurntableDrawMap = CollStreamUtil.toIdentityMap(map.values(), ConfigActivityTurntableDraw::getFunctionId);
  }

  /** 加载转盘抽奖活动数据 */
  private void loadRankReward() {
    Collection<ConfigActivityRank> values = getLubanLoader().getConfigActivityRankMap().values();
    if (CollUtil.isEmpty(values)) return;

    Table<Integer, String, RankConfigDTO> tmpRankMap = HashBasedTable.create();
    for (ConfigActivityRank rank : values) {
      tmpRankMap.put(rank.getFunctionId(), rank.getRankType(), new RankConfigDTO(rank));
    }
    funcIdRankTypeRankMap = tmpRankMap;
  }

  /** 获取活跃度进度奖励配置 */
  public static ConfigActivityProgressReward getProgressConfig(int funcId) {
    return progressRewardMap.get(funcId);
  }

  /** 获取指定的活跃度奖励配置 */
  public static ChildrenConfigActivityProgressRewardProgressList getProgressRewardConfig(
      int funcId, int id) {
    ConfigActivityProgressReward config = getProgressConfig(funcId);
    return config == null
        ? null
        : config.getProgressList().stream()
            .filter(c -> c.getProgressId() == id)
            .findFirst()
            .orElse(null);
  }



  /** 获取转盘抽奖活动配置 */
  public static ConfigActivityTurntableDraw getConfigActivityTurntableDraw(int funcId) {
    return funcIdTurntableDrawMap.get(funcId);
  }


  /** 获取排行榜活动配置 */
  public static RankConfigDTO getRankConfigDTO(int funcId, RankType rankType) {
    return funcIdRankTypeRankMap.get(funcId, rankType.name());
  }
}
