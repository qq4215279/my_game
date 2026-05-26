package com.mumu.game.business.item.domain;

import com.mumu.game.business.item.luban.ItemConfigManager;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * PlayerPack
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/26 18:20
 */
public class PlayerPack {

    private long playerId;

    private int itemId;

    /** stack_id:堆叠ID,计时/计数道具，堆叠id为0，限时计数道具堆叠id为过期时间戳 */
    private long stackId;

    /** count:道具数量 */
    private long count;

    /** expire:道具过期时间 */
    private long expire;

    /** newFlag:是否为新道具 */
    private int newFlag;

    /** field1:扩展字段1 */
    private int field1;

    /** field1:扩展字段2 */
    private int field2;

    /** 更新数量 */
    public void addCount(long addCount) {
        this.count = Math.max(0, this.count + addCount);
    }

    /** 增加有效期 */
    public void addExpire(long num, long addTime) {
        long finalAddTime = num * addTime;
        if (checkExpire()) {
            this.expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(finalAddTime);
        } else {
            this.expire += TimeUnit.SECONDS.toMillis(finalAddTime);
        }
    }

    /** 道具是否已过期 */
    public boolean checkExpire() {
        return expire != -1 && expire < System.currentTimeMillis();
    }

    /** 道具是永久道具 */
    public boolean isPermanent() {
        return expire == -1;
    }

    public boolean hasRedPoint() {
        return this.newFlag == 1 && this.count > 0 && isShowPack();
    }

    public boolean isShowPack() {
        return Optional.ofNullable(ItemConfigManager.getBaseItem(itemId))
                .map(BaseItem::isShowPack)
                .orElse(false);
    }

}
