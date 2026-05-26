package com.mumu.game.core.drop.item;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.item.PopGoodsReason;
import com.mumu.game.proto.message.core.ErrorCode;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

/**
 * DropItemResult TODO
 * 道具资源变动结果
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:41
 */
@Data
public class DropItemResult {
    /** 结果 */
    private ErrorCode code;

    /** 变动的资源Map */
    private Map<Integer, ItemResource> resourceMap = Collections.emptyMap();

    /** 弹出礼包信息 */
    private PopGoods popGoods = PopGoods.POP_GOODS;

    /** 获得的道具奖励信息 */
    private List<ItemBean> rewardItems = Collections.emptyList();

    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS;
    }

    public boolean isError() {
        return !isSuccess();
    }

    // ===================== 构造成功返回 =====================
    public static DropItemResult success(List<ItemBean> rewardItems) {
        return success(Collections.emptyMap(), rewardItems);
    }

    public static DropItemResult success(Map<Integer, ItemResource> resourceMap) {
        return success(resourceMap, Collections.emptyList());
    }

    public static DropItemResult success(Map<Integer, ItemResource> resourceMap, List<ItemBean> rewardItems) {
        return of(ErrorCode.SUCCESS, PopGoods.POP_GOODS, resourceMap, rewardItems);
    }

    // ===================== 构造失败返回 =====================
    public static DropItemResult error(ErrorCode code) {
        return error(code, PopGoods.POP_GOODS);
    }

    public static DropItemResult error(ErrorCode code, PopGoods popGoods) {
        return of(code, popGoods, Collections.emptyMap(), Collections.emptyList());
    }

    public static DropItemResult of(ErrorCode code, PopGoods popGoods, Map<Integer, ItemResource> resourceMap,
        List<ItemBean> rewardItems) {
        DropItemResult result = new DropItemResult();
        result.code = code;
        result.resourceMap = resourceMap;
        result.popGoods = popGoods;
        result.rewardItems = rewardItems;
        return result;
    }

    @Getter
    @ToString
    public static final class PopGoods {
        /**  */
        public static final PopGoods POP_GOODS = new PopGoods();

        /** 弹出礼包ID */
        List<Integer> popGoodsIdList;
        /** 冻结道具总数量 */
        long freezeItemNum = 0;
        /** 弹窗礼包原因列表 */
        List<PopGoodsReason> popGoodsReasonList;

        public static PopGoods of(List<Integer> popGoodsIdList, long freezeItemNum,
            List<PopGoodsReason> popGoodsReasonList) {
            PopGoods pg = new PopGoods();
            pg.popGoodsIdList = popGoodsIdList;
            pg.freezeItemNum = freezeItemNum;
            pg.popGoodsReasonList = popGoodsReasonList;
            return pg;
        }
    }
}
