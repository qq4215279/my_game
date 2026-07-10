package com.mumu.game.core.db.example;

import com.mumu.game.core.db.anno.Index;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Club
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/10 16:03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ModelTable(
        name = "club",
        comment = "公会表",
        persistStrategy = PersistStrategy.REDIS_DB,
        indexes = {
                @Index(name = "unq_clubid", value = {"clubId"})
        }
)
public class Club extends BaseEntity {
    /** 公会id */
    private long clubId;
    /** 公会名称 */
    private String clubName;
    /** 公会名称 */
    private String clubDesc;

    @Override
    public long getPrimaryRouteId() {
        return clubId;
    }
}
