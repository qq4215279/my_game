package com.mumu.game.mail.domain;

import com.game.framework.core.automodel.anno.AutoColumn;
import com.game.framework.core.automodel.anno.AutoTable;
import com.game.framework.core.automodel.consts.LoadStrategy;
import com.game.framework.core.automodel.consts.PersistStrategy;
import com.game.framework.core.automodel.entity.DataEntity;
import com.game.framework.net.consts.ServerGroup;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 玩家邮件表 @Date: 2024/9/2 下午3:11 @Author: xu.hai */
@EqualsAndHashCode(callSuper = true)
@Data
@AutoTable(
    name = "player_mail_idx",
    comment = "玩家邮件索引表",
    loadStrategy = LoadStrategy.SELECT_ONE,
    persistStrategy = PersistStrategy.REDIS_DB,
    belongs = ServerGroup.WORLD,
    preLoad = true)
public class PlayerMailIdxDO extends DataEntity {

  @AutoColumn(name = "player_id", dbDefault = "0", primaryKey = true, comment = "玩家id")
  private long playerId;

  @AutoColumn(name = "sys_mail_idx", dbDefault = "0", comment = "系统邮件索引id")
  private long sysMailIdx;

  @Override
  public Object getPrimaryKey() {
    return playerId;
  }

  @Override
  public long getDataId() {
    return playerId;
  }
}
