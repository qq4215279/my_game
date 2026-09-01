package com.mumu.game.rank.luban.dto;

import com.mumu.game.business.item.luban.ItemConfigManager;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.utils.CovertUtil;
import com.mumu.game.core.utils.MathUtil;
import com.mumu.game.rank.luban.bean.RankRewardBean;
import com.mumu.game.rank.luban.bean.RankRewardByPercentBean;
import org.apache.commons.lang3.StringUtils;


import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.ToString;

/** 奖励配置DTO @Date: 2025/6/27 下午4:25 @Author: xu.hai */
@Getter
@ToString
public class RankRewardDTO {

  private final int min;
  private final int max;

  /** 掉落奖励 */
  private RankRewardBean rewardBean;

  /** 百分比奖励 */
  private RankRewardByPercentBean percentBean;

  /** 掉落奖励 */
  private String rewards;

  /** 百分比奖励道具 */
  private int itemId;

  public RankRewardDTO(ChildrenConfigActivityRankRankRewards conf, boolean isPercent) {
    this.min = conf.getMin();
    this.max = conf.getMax();

    if (isPercent) parsePercent(conf);
    else parseReward(conf);
  }

  private void parseReward(ChildrenConfigActivityRankRankRewards conf) {
    RankRewardBean bean = new RankRewardBean();
    bean.setMin(conf.getMin());
    bean.setMax(conf.getMax());
    bean.setRewards(RewardEnum.toItemBeans(conf.getRewards()));
    this.rewardBean = bean;
    this.rewards = conf.getRewards();
  }

  private void parsePercent(ChildrenConfigActivityRankRankRewards conf) {
    // 1000,10  第一位ItemId，第二位瓜分比例
    int[] arr = CovertUtil.stringToIntArr(conf.getRewards(), Symbol.COMMA);
    Assert.isTrue(arr.length == 2, "排行榜百分比奖励配置有误! reward: {}", conf.getRewards());
    Assert.notNull(
        ItemConfigManager.getBaseItem(arr[0]), "排行榜百分比奖励配置有误! reward: {}", conf.getRewards());

    RankRewardByPercentBean bean = new RankRewardByPercentBean();
    bean.setMin(conf.getMin());
    bean.setMax(conf.getMax());
    bean.setPercent(arr[1]);
    this.percentBean = bean;
    this.itemId = arr[0];
  }

  /** 计算百分比奖励（pool：瓜分奖励） */
  public long calPercentNum(long pool) {
    if (percentBean == null) return 0;
    // return (long) (pool * (double) percentBean.getPercent() / CoreConstants.GAME_PERCENT_RATE);
    return MathUtil.calPercent(pool, percentBean.getPercent());
  }

  /** 计算百分比奖励，返回奖励串（pool：瓜分奖励） */
  public String calPercentReward(long pool) {
    long num = calPercentNum(pool);
    return num >= 0 ? RewardEnum.buildReward(itemId, num) : StringUtils.EMPTY;
  }
}
