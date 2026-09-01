package com.mumu.game.rank.luban.dto;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cn.hutool.core.lang.Assert;
import com.mumu.game.constants.CoreConstants;
import com.mumu.game.rank.luban.bean.RankRewardByPercentBean;
import lombok.Getter;

/**
 * RankConfigDTO
 *
 * @author liuzhen
 * @version 1.0.0 2025/6/26 20:29
 */
@Getter
public class RankConfigDTO {
  /** 功能id */
  private final int functionId;

  /** 榜单类型 */
  private final RankType rankType;

  /** 发奖形式 0: 掉落; 1: 百分比 */
  private final int rewardType;

  /** 发奖形式为1，百分比掉落时，获得的道具ID */
  private int itemId;

  /** 描述 */
  private final String desc;

  /** 邮件后缀 */
  private final String mailSuffix;

  /** 排行榜奖励 */
  private final TreeMap<Integer, RankRewardDTO> rankRewardMap;

  public RankConfigDTO(ConfigActivityRank conf) {
    this.functionId = conf.getFunctionId();
    this.rankType = RankType.valueOf(conf.getRankType());
    this.rewardType = Integer.parseInt(conf.getRewardType());
    this.desc = conf.getDesc();
    this.mailSuffix = conf.getMailSuffix();
    boolean isPercent = isPercent();
    this.rankRewardMap =
        conf.getRankRewards().stream()
            .map(c -> new RankRewardDTO(c, isPercent))
            .collect(Collectors.toMap(RankRewardDTO::getMin, o -> o, (o1, o2) -> o1, TreeMap::new));
    // 比例瓜分类型下，记录道具ID
    if (isPercent) {
      // 检查发放比例不能超过100
      int sumPercent =
          rankRewardMap.values().stream()
              .map(RankRewardDTO::getPercentBean)
              .mapToInt(RankRewardByPercentBean::getPercent)
              .sum();
      Assert.isTrue(
          0 <= sumPercent && sumPercent <= CoreConstants.GAME_PERCENT_RATE,
          "排行榜百分比奖励配置有误! sumPercent: {}",
          sumPercent);

      var entry = this.rankRewardMap.firstEntry();
      if (entry != null) this.itemId = entry.getValue().getItemId();
    }
  }

  public List<RankRewardDTO> getRankRewards() {
    return Lists.newArrayList(rankRewardMap.values());
  }

  /** 获取最后一名奖励的名次 */
  public int getRewardMaxRank() {
    var last = rankRewardMap.lastEntry();
    return last == null ? 0 : last.getValue().getMax();
  }

  /** 获取排行榜奖励配置 */
  public RankRewardDTO getRewardConf(int rank) {
    // 查找指定rank
    var entry = rankRewardMap.floorEntry(rank);
    // 小于最大配置值
    return entry != null && rank <= entry.getValue().getMax() ? entry.getValue() : null;
  }

  /** 是否为比例瓜分类型 */
  public boolean isPercent() {
    return rewardType == 1;
  }

  /** 发奖形式为1，百分比掉落时，构造的道具信息 */
  public String buildReward(long num) {
    return RewardEnum.buildReward(itemId, num);
  }
}
