package com.mumu.game.core.db;

import com.mumu.game.core.db.anno.Index;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;

/**
 * 仅 DB 策略单测实体（无 JVM / Redis）
 */
@ModelTable(
    name = "db_only_player",
    comment = "仅DB玩家表",
    persistStrategy = PersistStrategy.DB,
    preLoad = true,
    indexes = {
        @Index(name = "unq_playerid", value = {"playerId"})
    }
)
public class DbOnlyPlayerEntity extends BaseEntity {
    private long playerId;
    private String name;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getPrimaryRouteId() {
        return playerId;
    }
}
