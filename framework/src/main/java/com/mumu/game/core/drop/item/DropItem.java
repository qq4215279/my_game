package com.mumu.game.core.drop.item;

import cn.hutool.core.lang.Assert;
import com.mumu.game.business.item.luban.ItemConfigManager;
import com.mumu.game.core.drop.consts.DropMethod;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.drop.consts.ItemId;
import com.mumu.game.core.drop.core.ItemFlag;
import com.mumu.game.core.log.DropAction;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.message.core.ErrorCode;
import lombok.Getter;

/**
 * DropItem
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:45
 */
@Getter
public abstract class DropItem {
    /** 奖励类型 */
    protected DropType type;
    /** id */
    protected int id;
    /** 数量 */
    protected long num;
    /** 时间【秒级】 */
    protected long addTime;

    /** 掉落方法【默认必掉】：1-必掉; 2-独立按概率掉落; 3-多选一  */
    protected DropMethod dropMethod = DropMethod.MUST_DROP;
    /** 掉落概率【默认1】 */
    protected double prob = 1;


    /** 通过道具ID构造奖励类型（用于获取） */
    public DropItem(int id) {
        this.id = id;
        this.type = ItemConfigManager.getRewardType(id);
    }

    /** 解析奖励字符串 */
    public boolean parseReward(String[] rewardField) {
        if (rewardField == null || rewardField.length < 2) {
            return false;
        }
        if (Integer.parseInt(rewardField[0]) != this.id) {
            return false;
        }
        this.num = Long.parseLong(rewardField[1]);
        long addTime = -1;
        if (rewardField.length > 2) {
            addTime = Integer.parseInt(rewardField[2]);
        }
        this.addTime = addTime;
        return true;
    }

    /** 获取玩家指定货币bean 注：num数量为当前玩家拥有总数; expireTime 为道具过期时间 */
    public ItemBean getOwnItem(long playerId) {
        ItemBean bean = new ItemBean();
        bean.setId(id);
        bean.setNum(getOwnNum(playerId));
        bean.setExpireTime(getExpireTime(playerId));
        bean.setHasPoint(hasRedPoint(playerId));
        return bean;
    }

    /** 获取道具ItemBean对象 注：num数量为掉落数量; */
    public ItemBean toItemBean() {
        ItemBean bean = new ItemBean();
        bean.setId(id);
        bean.setNum(num);
        bean.setTime(addTime);
        return bean;
    }

    /** 默认返回ItemBean */
    protected ItemBean defaultItemBean() {
        return toItemBean();
    }

    /** 奖励格式（特殊格式需要自己重写） */
    public String toReward() {
        return ItemId.buildReward(id, num, addTime);
    }

    /** 道具掉落给玩家 */
    public abstract boolean drop(long playerId, DropAction action);

    /** 使用道具 */
    public DropItemResult use(long playerId, DropAction action) {
        return DropItemResult.error(ErrorCode.FAIL);
    }

    /** 扣除玩家当前道具数量（默认扣除背包道具） deductNum-正数 */
    public abstract boolean deduct(long playerId);

    /** 清除玩家当前道具所有数量 */
    public abstract ItemBean clear(long playerId);

    /** 获取玩家该道具的数量（默认获取背包道具） */
    public abstract long getOwnNum(long playerId);


    /** 是否有红点 */
    public boolean hasRedPoint(long playerId) {
        return false;
    }

    /** 获取道具过期时间 */
    public long getExpireTime(long playerId) {
        return -1;
    }

    /** 道具数量翻倍 */
    public void multi(int multi) {
        this.num *= multi;
    }

    /** 奖励道具合并 */
    public DropItem merge(DropItem reward) {
        Assert.isTrue(type.isMerge() && reward.getId() == id, "道具不能合并! itemA: {}, itemB: {}", this, reward);

        if (ItemFlag.COUNT.isCurr(id)) {
            // 计数道具
            this.num += reward.getNum();
        } else {
            // 计时道具
            if (num > 1) {
                this.addTime *= this.num;
                this.num = 1;
            }

            if (this.addTime == -1 || reward.getAddTime() == -1) {
                this.addTime = -1;
            } else {
                this.addTime += reward.getAddTime() * reward.getNum();
            }
        }
        return this;
    }
}
