package com.mumu.game.rank.rank;

import java.util.Map;

import com.mumu.game.core.log.LogAction;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.timer.consts.Cron;
import org.springframework.stereotype.Component;

import com.google.common.collect.Table;

/**
 * 排行榜结算任务
 *
 * @author liuzhen
 * @version 1.0.0 2025/6/24 15:35
 */
@Component
public class RankSettleScheduler {

  /** 排行榜活动发奖任务 */
  @AutoScheduled(key = AutoScheduleKey.RANK_ACTIVITY_SETTLE, cron = Cron.EVERY_DAY_0_0)
  public void sendRankActivityReward() {
    for (Table.Cell<Integer, String, RankConfigDTO> cell :
        ActivityConfigManager.getFuncIdRankTypeRankMap().cellSet()) {
      RankConfigDTO rankDTO = cell.getValue();
      // 活动是否开启
      if (!PeriodActivityConfigManage.hasActivity(cell.getRowKey())) {
        LogTopic.ACTION.warn(LogAction.RANK, "sendRankActivityReward", "no Open", "dto", rankDTO);
        continue;
      }
      try {
        String errMsg =
            rankDTO.isPercent() ? sendRankReward4Percent(rankDTO) : sendRankReward4DropStr(rankDTO);
        if (errMsg != null) {
          LogTopic.ACTION.warn(LogAction.RANK, "sendRankReward", errMsg, "rankDTO", rankDTO);
        }
      } catch (Exception e) {
        LogTopic.ACTION.error(e, LogAction.RANK, "sendRankReward", "dto", rankDTO);
      }
    }
  }

  /** 发奖By掉落 */
  private String sendRankReward4DropStr(RankConfigDTO rankDTO) {
    int functionId = rankDTO.getFunctionId();
    int maxRank = rankDTO.getRewardMaxRank();
    if (maxRank <= 0) return "排行榜奖励配置有误";
    RankFunc rankFunc = RankFunc.getRankFunc(functionId);
    if (rankFunc == null) return "未找到功能对应排行榜";

    RankType rankType = rankDTO.getRankType();
    // 获取上一期所有发奖玩家
    Map<Long, RankScoreBean> playerScoreMap =
        rankFunc.getRedisRank().getTopNMap(maxRank, rankType, rankType.getPrevPeriodId());
    for (Map.Entry<Long, RankScoreBean> entry : playerScoreMap.entrySet()) {
      RankScoreBean rankScoreBean = entry.getValue();
      RankRewardDTO rewardDTO = rankDTO.getRewardConf(rankScoreBean.getRank());
      // 未找到奖励
      if (rewardDTO == null) continue;

      // 机器人实际不发邮件
      if (PlayerUtil.isRobot(entry.getKey())) continue;
      // 发送邮件 mailTitle mailContent
      MailParams.build()
          .setMailType(MailType.RANK)
          .setMailLanguageEnumSuffixName(rankDTO.getMailSuffix())
          .appendAgr(rankScoreBean.getRank())
          .setGive(rewardDTO.getRewards())
          .sendMail(entry.getKey());
    }
    LogTopic.ACTION.info(
        LogAction.RANK,
        "排行榜结算DropStr",
        "functionId",
        functionId,
        "maxRank",
        maxRank,
        "rankFunc",
        rankFunc,
        "rankType",
        rankType,
        "playerIds",
        playerScoreMap.keySet());
    return null;
  }

  /** 发奖By百分比 */
  private String sendRankReward4Percent(RankConfigDTO rankDTO) {
    int functionId = rankDTO.getFunctionId();
    int maxRank = rankDTO.getRewardMaxRank();
    if (maxRank <= 0) return "排行榜奖励配置有误";

    RankFunc rankFunc = RankFunc.getRankFunc(functionId);
    if (rankFunc == null) return "未找到功能对应的排行榜";

    RankType rankType = rankDTO.getRankType();
    RedisPool redisPool = rankFunc.getType(rankType).getRedisPool();
    if (redisPool == null) return "未找到排行榜功能对应的奖池";

    // 之前的奖池数，放出的奖池数
    long beforePool = redisPool.get(), totalOut = 0;

    // 获取上一期所有发奖玩家
    Map<Long, RankScoreBean> playerScoreMap =
        rankFunc.getRedisRank().getTopNMap(maxRank, rankType, rankType.getPrevPeriodId());
    for (Map.Entry<Long, RankScoreBean> entry : playerScoreMap.entrySet()) {
      RankScoreBean rankScoreBean = entry.getValue();
      RankRewardDTO rewardDTO = rankDTO.getRewardConf(rankScoreBean.getRank());
      // 未找到奖励
      if (rewardDTO == null) continue;
      // 计算奖励
      long sendNum = rewardDTO.calPercentNum(beforePool);
      if (sendNum <= 0) continue;
      totalOut += sendNum;

      // 机器人实际不发邮件
      if (PlayerUtil.isRobot(entry.getKey())) continue;
      // 发送邮件 mailTitle mailContent
      MailParams.build()
          .setMailType(MailType.RANK)
          .setMailLanguageEnumSuffixName(rankDTO.getMailSuffix())
          .appendAgr(rankScoreBean.getRank())
          .setGive(rankDTO.buildReward(sendNum))
          .sendMail(entry.getKey());
    }

    // 重置榜单金额
    redisPool.decr(beforePool);
    LogTopic.ACTION.info(
        LogAction.RANK,
        "排行榜结算Percent",
        "functionId",
        functionId,
        "maxRank",
        maxRank,
        "rankFunc",
        rankFunc,
        "rankType",
        rankType,
        "beforePool",
        beforePool,
        "totalOut",
        totalOut,
        "playerIds",
        playerScoreMap.keySet());
    return null;
  }
}
