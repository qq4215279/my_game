package com.mumu.game.core.drop.condition;

import cn.hutool.core.lang.Pair;
import com.mumu.game.core.drop.consts.ItemId;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.drop.item.DropItemResult;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.proto.item.PopGoodsReason;
import com.mumu.game.proto.message.core.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DeductConditionManager
 * 道具扣除条件管理
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:55
 */
@Component
public class DeductConditionManager {
    static List<DeductCondition> CONDITIONS = Collections.emptyList();

    @Autowired(required = false)
    public void setConditions(List<DeductCondition> conditions) {
        DeductConditionManager.CONDITIONS = conditions;
    }

    /**
     * 检查道具是否足够
     *
     * @param playerId     playerId
     * @param pushPopGoods 是否推送弹窗礼包
     * @param drop         掉落
     * @return key-错误码; value: 弹窗礼包id
     */
    public static Pair<ErrorCode, DropItemResult.PopGoods> checkDeductItem(
            long playerId, boolean pushPopGoods, Drop drop, DropAction dropAction) {
        for (DropItem dropItem : drop.getRewardList()) {
            Pair<ErrorCode, DropItemResult.PopGoods> pair =
                    checkDeductItem(playerId, pushPopGoods, dropItem.getId(), dropItem.getNum(), dropAction);
            if (pair.getKey() != ErrorCode.SUCCESS) {
                return pair;
            }
        }
        return Pair.of(ErrorCode.SUCCESS, DropItemResult.PopGoods.POP_GOODS);
    }

    /**
     * 检查道具是否足够
     *
     * @param playerId     playerId
     * @param pushPopGoods 是否推送弹窗礼包
     * @param itemId       道具id
     * @param costCount    扣除数量
     * @return key-错误码; value: 弹窗礼包id
     */
    public static Pair<ErrorCode, DropItemResult.PopGoods> checkDeductItem(
            long playerId, boolean pushPopGoods, int itemId, long costCount, DropAction dropAction) {
        // 道具不足
        long ownNum = ItemId.getOwnNum(playerId, itemId);

        // 弹出原因
        List<PopGoodsReason> reasonList = new ArrayList<>();
        // pair: key-成功失败; value-冻结道具数量
        Pair<Boolean, Long> pair = checkSuccess(playerId, itemId, ownNum, costCount, reasonList);
        long freezeItemNum = pair.getValue();
        // 校验失败
        if (!pair.getKey()) {
            long diff = ownNum - costCount - freezeItemNum;
            // List<Integer> popGoodsIdList = pushPopGoods ? PopGoodsHelper.findAndPushPopGoods(playerId, itemId, diff, freezeItemNum, reasonList, currencyAction) : PopGoodsHelper.findPopGoodsIdList(itemId, diff);
            List<Integer> popGoodsIdList = Collections.emptyList();

            return Pair.of(ErrorCode.FAIL_ITEM_NOT_ENOUGH, DropItemResult.PopGoods.of(popGoodsIdList, freezeItemNum, reasonList));
        }

        return Pair.of(ErrorCode.SUCCESS, DropItemResult.PopGoods.POP_GOODS);
    }

    /**
     * 检查道具是否充足
     *
     * @param playerId   playerId
     * @param itemId     道具id
     * @param ownNum     拥有数量
     * @param costCount  扣除数量
     * @param reasonList 原因列表
     * @return key-是否充足; value: 总冻结数量
     */
    private static Pair<Boolean, Long> checkSuccess(
            long playerId, int itemId, long ownNum, long costCount, List<PopGoodsReason> reasonList) {
        long totalFreezeGoldNum = 0;
        // 未冻结判定
        if (!defaultCheckDeductItem(ownNum, costCount, totalFreezeGoldNum)) {
            reasonList.add(PopGoodsReason.POP_REASON_COMMON);
        }

        for (DeductCondition condition : CONDITIONS) {
            // 填充冻结金币
            long freezeItemNum = condition.getFreezeItemNum(playerId, itemId, ownNum, costCount);
            // 不存在冻结金币
            if (freezeItemNum <= 0) {
                continue;
            }
            totalFreezeGoldNum += freezeItemNum;
            if (!defaultCheckDeductItem(ownNum, costCount, totalFreezeGoldNum)) {
                reasonList.add(condition.getPopGoodsReason());
                return Pair.of(false, totalFreezeGoldNum);
            }
        }

        if (!reasonList.isEmpty()) {
            return Pair.of(false, totalFreezeGoldNum);
        }

        return Pair.of(true, 0L);
    }

    /**
     * 是否满足扣除
     *
     * @param ownNum        当前拥有数量
     * @param costCount     需要扣除数量
     * @param freezeGoldNum 冻结金币数量
     * @return boolean
     */
    private static boolean defaultCheckDeductItem(long ownNum, long costCount, long freezeGoldNum) {
        return ownNum >= costCount + freezeGoldNum;
    }

}
