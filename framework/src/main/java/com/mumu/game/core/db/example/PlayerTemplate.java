package com.mumu.game.core.db.example;

import com.mumu.game.core.db.anno.Index;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * PlayerTemplate
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9 17:19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ModelTable(
    name = "player_template", comment = "玩家模版表",
    persistStrategy = PersistStrategy.REDIS_DB,
    preLoad = true,
    indexes = {
            @Index(name = "playerid_functionid", value = {"playerId", "functionId"}),
            @Index(name = "playerid_activityid", value = {"playerId", "activityId"}),
    }
)
public class PlayerTemplate extends BaseEntity {
    /** 玩家id */
    private long playerId;
    /** 功能id */
    private int functionId;
    /** 活动id */
    private int activityId;
    /** 上次每日重置时间 */
    private long lastResetTime;
    /** 是否赛季结算过 0：否；1已结算 */
    private long seasonCaclState;
    /** 参数。注：只用于记录简单数据，复杂信息走协议！ */
    private String param = StringUtils.EMPTY;


    @Override
    public long getPrimaryRouteId() {
        return playerId;
    }
}
