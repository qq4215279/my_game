package com.mumu.game.core.db.example;

import com.mumu.game.core.db.anno.Index;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Player
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9 17:09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ModelTable(
     name = "player",
     comment = "玩家表",
     persistStrategy = PersistStrategy.REDIS_DB,
     indexes = {
             @Index(name = "unq_playerid", value = {"playerId"})
     }
)
public class Player extends BaseEntity {
    /** 玩家id */
    private long playerId;
    /** 玩家名称 */
    private String name;
    /** 玩家等级 */
    private int level;


    @Override
    public long getPrimaryRouteId() {
        return playerId;
    }
}
