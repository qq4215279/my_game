package com.mumu.game.business.item;

import com.mumu.game.core.drop.core.ItemFlag;

import lombok.Data;

/**
 * BaseItem
 * 道具基础信息
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:08
 */
@Data
public class BaseItem {
    /** 道具id */
    private int itemId;

    /** 道具名称 */
    private String name;

    /** 道具备注（中文名） */
    private String remark;

    /** 道具类型 */
    private int itemType;

    /** 是否在背包中显示（0-否，1-是） */
    private boolean showPack;

    /** 道具标识 */
    private ItemFlag itemFlag;

    /** 品质 */
    private int quality;

    /** 堆叠上限 -1表无上限 */
    private int posLimit;

    /** 附加道具掉落 */
    private String attachDropStr;

    // public BaseItem(ConfigItem conf) {
    //     this.itemId = Integer.parseInt(conf.getData_id());
    //     this.name = conf.getName();
    //     this.remark = conf.getRemark();
    //     this.itemType = Integer.parseInt(conf.getDropType());
    //     Assert.notNull(
    //             RewardType.getRewardType(itemType),
    //             "无效的道具类型 itemId: {}, type: {}",
    //             conf.getData_id(),
    //             conf.getDropType());
    //     this.showPack = conf.getShowPack();
    //     this.itemFlag = ItemFlag.valueOf(conf.getItemFlag());
    //     this.quality = conf.getQuality();
    //     this.posLimit = conf.getPosLimit();
    //     this.attachDropStr = conf.getAttachDropStr();
    // }
}
