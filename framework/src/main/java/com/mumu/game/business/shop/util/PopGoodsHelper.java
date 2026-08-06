package com.mumu.game.business.shop.util;

import cn.hutool.core.collection.CollUtil;
import com.game.business.item.reward.consts.RewardEnum;
import com.game.business.item.reward.drop.condition.DeductConditionManager;
import com.game.business.player.manager.PlayerManager;
import com.game.business.player.util.PlayerUtil;
import com.game.business.shop.luban.ShopConfigManager;
import com.game.framework.core.log.CurrencyAction;
import com.game.framework.net.consts.ServerGroup;
import com.game.framework.rpc.RpcManager;
import com.game.framework.rpc.api.world.WorldBaseRpcFunc;
import com.game.proto.core.ErrorCode;
import com.game.proto.shop.PopGoodsReason;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * PopGoodsHelper 弹窗礼包帮助类
 *
 * @author liuzhen
 * @version 1.0.0 2024/10/21 16:58
 */
public final class PopGoodsHelper {

  /** 检查玩家金币不足，并推送弹窗礼包 */
  public static ErrorCode checkGold(long playerId, long limitGold, CurrencyAction currencyAction) {
    return DeductConditionManager.checkDeductItem(
        playerId, true, RewardEnum.GOLD.getItemId(), limitGold, currencyAction).getKey();
  }

  /**
   * 查找并推送弹窗礼包
   * @param playerId playerId
   * @param itemId   itemId
   * @param diff     diff
   * @return int 弹窗礼包id列表
   * @since 2024/10/21 19:28
   */
  public static List<Integer> findAndPushPopGoods(long playerId, int itemId, long diff,
      long freezeGoldNum, List<PopGoodsReason> reasonList, CurrencyAction currencyAction) {

    if (PlayerUtil.isRobot(playerId)) {
      return Collections.emptyList();
    }

    List<Integer> popGoodsIdList = findPopGoodsIdList(itemId, diff);
    if (popGoodsIdList.isEmpty()) {
      return Collections.emptyList();
    }

    int serverId = PlayerManager.self().getServerId(playerId, ServerGroup.WORLD);
    // 玩家不在线
    if (serverId == 0) {
      return Collections.emptyList();
    }

    RpcManager.getProxy(WorldBaseRpcFunc.class).triggerPopGoods(playerId, popGoodsIdList, freezeGoldNum, reasonList, currencyAction);

    // AWSendOnPushPopGoodsMessage sendMsg = new AWSendOnPushPopGoodsMessage();
    // sendMsg.setGoodsIdList(popGoodsIdList);
    // sendMsg.setTargetPlayerId(playerId);
    // sendMsg.setFreezeItemNum(freezeGoldNum);
    // sendMsg.getPopGoodsReasonList().addAll(reasonList);
    //
    // // TODO 给玩家转到指定服操作
    // // 1. 玩家在本服，直接转到本服玩家线程处理
    // if (serverId == CoreConfig.getServerId()) {
    //   // 转入玩家线程处理
    //   MessageSender.sendRunNow(playerId, Cmd.AWSendOnPushPopGoods, sendMsg);
    //   return popGoodsIdList;
    // }
    //
    // // 2. 玩家在其他服，发送到对应的服务处理
    // MessageSender.sendToPlayerServer(
    //     ServerGroup.WORLD, playerId, Cmd.AWSendOnPushPopGoods, sendMsg);

    return popGoodsIdList;
  }

  /**
   * 查找弹窗礼包商品id
   *
   * @param itemId 道具id
   * @param diff 不足道具数量
   * @return int 道具礼包id
   * @since 2024/10/10 11:26
   */
  public static int findPopGoodsId(int itemId, long diff) {
    TreeMap<Long, Integer> popGoodsMap = ShopConfigManager.getPopGoodsMap(itemId);
    if (CollUtil.isEmpty(popGoodsMap)) return 0;

    diff = Math.abs(diff);
    Entry<Long, Integer> entry = popGoodsMap.ceilingEntry(diff);
    if (entry == null) entry = popGoodsMap.floorEntry(diff);
    return entry == null ? 0 : entry.getValue();
  }

  /**
   * 查找推荐礼包列表
   * @param itemId 道具id
   * @param diff 不足道具数量
   * @return java.util.List<java.lang.Integer> 推荐道具礼包id
   * @since 2025/7/9 15:28
   */
  public static List<Integer> findPopGoodsIdList(int itemId, long diff) {
    TreeMap<Long, Integer> popGoodsMap = ShopConfigManager.getPopGoodsMap(itemId);
    if (CollUtil.isEmpty(popGoodsMap)) return Collections.emptyList();

    diff = Math.abs(diff);
    Entry<Long, Integer> entry = popGoodsMap.ceilingEntry(diff);
    if (entry == null) entry = popGoodsMap.floorEntry(diff);
    if (entry == null) return Collections.emptyList();

    List<Integer> result = new ArrayList<>();
    // 推荐1
    result.add(entry.getValue());

    // 推荐2
    Entry<Long, Integer> entry2 = popGoodsMap.higherEntry(entry.getKey());
    if (entry2 != null) {
      result.add(entry2.getValue());

      // 推荐3
      Entry<Long, Integer> entry3 = popGoodsMap.higherEntry(entry2.getKey());
      if (entry3 != null) {
        result.add(entry3.getValue());
      }
    }

    return result;
  }
}
