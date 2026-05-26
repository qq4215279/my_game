package com.mumu.game.core.drop.item;

import com.mumu.game.proto.item.ItemBean;
import lombok.Data;
import lombok.ToString;

/**
 * ItemResource
 * 道具资源信息
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:56
 */
@Data
@ToString
public class ItemResource {
    /**
     * 道具id
     */
    private int itemId;

    /**
     * 最新数量
     */
    private long num;

    /**
     * 过期时间 永久:-1（秒）
     */
    private long expireTime;

    /**
     * 变动数量（负数表示减少）
     */
    private long change;

    /**
     * 是否有红点
     */
    private boolean hasPoint;

    /**
     * 构造玩家当前道具信息
     */
    public ItemBean toOwnItemBean() {
        ItemBean itemBean = new ItemBean();
        itemBean.setId(itemId);
        itemBean.setNum(num);
        itemBean.setExpireTime(expireTime);
        itemBean.setHasPoint(hasPoint);
        return itemBean;
    }

    public static ItemResource of(int itemId, long num, long expireTime, long change, boolean hasPoint) {
        ItemResource resource = new ItemResource();
        resource.itemId = itemId;
        resource.num = num;
        resource.expireTime = expireTime;
        resource.change = change;
        resource.hasPoint = hasPoint;
        return resource;
    }

}
