package com.mumu.game.shop.util;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.proto.shop.OnPushPopGoodsMessage;
import com.mumu.game.proto.shop.PopGoodsReason;

import java.util.List;

/**
 * ShopUtil
 * 商城工具类
 * @author liuzhen
 * @version 1.0.0 2025/7/8 18:25
 */
public class ShopUtil {


  /**
   * 请求发送推送弹窗礼包
   * @param targetPlayerId targetPlayerId
   * @param goodsIdList 弹窗礼包id列表
   * @param freezeItemNum 冻结道具总数量
   * @param popGoodsReasonList 弹窗礼包原因列表
   * @since 2025/7/8 18:27
   */
  /*public static void sendOnPushPopGoods(long targetPlayerId, List<Integer> goodsIdList, long freezeItemNum, List<PopGoodsReason> popGoodsReasonList, CurrencyAction currencyAction) {
    if (goodsIdList.isEmpty()) {
      return;
    }

    OnPushPopGoodsMessage pushMsg = new OnPushPopGoodsMessage();

    for (int popGoodsId : goodsIdList) {
      // 构建弹出礼包信息
      ConfigShopDTO popShop = ShopConfigManager.getConfigShop(popGoodsId);
      if (popShop == null) {
        continue;
      }

      ShopTemp shopTemp = ShopTemplateManager.getShopTemplateByGoodsId(popGoodsId);
      pushMsg.getPopGoodsList().add(shopTemp.buildGoodsBean(targetPlayerId, popGoodsId));
    }

    pushMsg.setFreezeItemNum(freezeItemNum);
    pushMsg.getPopGoodsReasonList().addAll(popGoodsReasonList);
    pushMsg.setParam(currencyAction == null ? "" : currencyAction.name());

    // 推送
    MessageSender.pushToPlayer(targetPlayerId, Cmd.OnPushPopGoods, pushMsg);
  }*/

}
