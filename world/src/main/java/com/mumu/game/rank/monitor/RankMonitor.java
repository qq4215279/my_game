package com.mumu.game.rank.monitor;

import java.util.List;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.game.business.item.data.ItemResource;
import com.game.business.item.event.ItemChangeEvent;
import com.game.business.item.reward.consts.RewardEnum;
import com.game.business.player.bean.Player;
import com.game.business.rank.enums.RankFunc;
import com.game.business.rank.enums.RankType;
import com.game.business.rank.util.RankUtil;
import com.game.framework.core.cmd.consts.Cmd;
import com.game.framework.core.log.CurrencyAction;
import com.game.framework.core.redis.RedisUtil;
import com.game.framework.net.server.MessageSender;
import com.game.proto.rank.OnZCPushNearRankMessage;
import com.game.proto.rank.RankScoreBean;
import com.game.proto.rank.RankTypeEnum;
import com.game.template.func.core.FunctionIdEnum;

/** 排行榜进度增加事件监听 @Date: 2024/11/29 下午6:11 @Author: xu.hai */
@Component
public class RankMonitor {
  //
  // @Override
  // public boolean access(Player player, ActionType type) {
  //   return type == ActionType.REWARD_ITEM || type == ActionType.CONSUME_ITEM;
  // }
  //
  // @Override
  // public void accept(Player player, ActionData data) {
  //   // 机器人变动金币，不计入榜单统计
  //   if (CurrencyAction.ROBOT_CHANGE_GOLD_REWARD.ordinal() == data.getChange()) {
  //     return;
  //   }
  //
  //   switch (data.getType()) {
  //     case REWARD_ITEM -> {
  //       // baloot - 游戏获得金币奖励
  //       CurrencyAction action = CurrencyAction.get((int) data.getChange());
  //       if (action == CurrencyAction.BALOOT_WIN_GAME_REWARD) {
  //         incr(data, player.getPlayerId(), RankFunc.GOLD, RewardEnum.GOLD);
  //         pushPlayerGoldRankChange(player.getPlayerId());
  //
  //         // 水果机 - 赢得金币
  //       } else if (action == CurrencyAction.FRUIT_WIN_GAME_REWARD) {
  //         incr(data, player.getPlayerId(), RankFunc.FRUIT_GOLD, RewardEnum.GOLD);
  //       }
  //
  //       incr(data, player.getPlayerId(), RankFunc.EXP, RewardEnum.PLAYER_EXP);
  //       incr(data, player.getPlayerId(), RankFunc.VIP, RewardEnum.PLAYER_VIP_EXP);
  //       incr(data, player.getPlayerId(), RankFunc.CHARM, RewardEnum.CHARM);
  //     }
  //     case CONSUME_ITEM -> decr(data, player.getPlayerId(), RankFunc.CHARM, RewardEnum.CHARM);
  //   }
  // }
  //
  // private void incr(ActionData data, long playerId, RankFunc func, RewardEnum item) {
  //   long add = data.getInfoMap().getOrDefault(item.getItemId(), 0L);
  //   if (add > 0) func.incr(playerId, add);
  // }
  //
  // private void decr(ActionData data, long playerId, RankFunc func, RewardEnum item) {
  //   long add = data.getInfoMap().getOrDefault(item.getItemId(), 0L);
  //   func.incr(playerId, add);
  // }

  /** 排除的变动 */
  static final Set<CurrencyAction> EXCLUDE =
      Set.of(
          CurrencyAction.ROBOT_CHANGE_GOLD_REWARD,
          CurrencyAction.ROBOT_CHANGE_GOLD_CONSUME,
          CurrencyAction.ROBOT_BET_BROKEN_GOLD_REWARD);

  /** 金币道具消耗时检查破产 */
  @EventListener(ItemChangeEvent.class)
  public void onItemChangeEvent(ItemChangeEvent event) {
    CurrencyAction action = event.getAction();
    if (EXCLUDE.contains(action)) return;

    // baloot - 游戏获得金币奖励
    if (CurrencyAction.BALOOT_WIN_GAME_REWARD == action) {
      // 更新金币榜
      tryUpdate(event.getPlayer(), event.getResource(), RewardEnum.GOLD, RankFunc.GOLD);
      // 推送玩家金币排行榜变更
      pushPlayerGoldRankChange(event.getPlayerId());
    }

    // 获得道具时触发
    if (event.isReceived()) {
      // 更新经验榜
      tryUpdate(event.getPlayer(), event.getResource(), RewardEnum.PLAYER_EXP, RankFunc.EXP);
      // 更新vip经验榜
      tryUpdate(event.getPlayer(), event.getResource(), RewardEnum.PLAYER_VIP_EXP, RankFunc.VIP);
    }
    // 更新魅力值榜
    tryUpdate(event.getPlayer(), event.getResource(), RewardEnum.CHARM, RankFunc.CHARM);
  }

  /**
   * 变更排行榜单积分
   * @param player player
   * @param resource resource
   * @param item item
   * @param func func
   * @since 2025/8/1 17:40
   */
  private void tryUpdate(Player player, ItemResource resource, RewardEnum item, RankFunc func) {
    if (resource.getItemId() == item.getItemId()) {
      func.update(player.getPlayerId(), resource.getChange(), resource.getNum());
    }
  }

  /** 推送玩家金币排行榜变更 */
  private void pushPlayerGoldRankChange(long playerId) {
    OnZCPushNearRankMessage pushMsg = new OnZCPushNearRankMessage();
    pushMsg.setFunctionId(FunctionIdEnum.RANK_GOLD.getFunctionId());
    pushMsg.setType(RankTypeEnum.DAILY);

    List<RankScoreBean> rankScoreBeans =
        RedisUtil.getNearRankListByPlayerId(RankFunc.GOLD.getRankKey(RankType.DAILY), playerId);
    pushMsg.getNearRankList().addAll(RankUtil.createSimplePlayerRanks(rankScoreBeans).values());
    MessageSender.pushToPlayer(playerId, Cmd.OnZCPushNearRank, pushMsg);
  }
}
