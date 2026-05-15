package com.mumu.game.mail.domain;

import com.game.consts.Symbol;
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
    name = "player_mail",
    comment = "玩家邮件表",
    loadStrategy = LoadStrategy.SELECT_MANY,
    persistStrategy = PersistStrategy.REDIS_DB,
    belongs = ServerGroup.WORLD,
    preLoad = true)
public class PlayerMailDO extends DataEntity {

  @AutoColumn(name = "player_id", dbDefault = "0", primaryKey = true, order = 0, comment = "玩家id")
  private long playerId;

  @AutoColumn(name = "mail_id", dbDefault = "0", primaryKey = true, order = 1, comment = "邮件id")
  private long mailId;

  @AutoColumn(name = "mail_type", dbDefault = "0", comment = "邮件类型")
  private int mailType;

  @AutoColumn(name = "title", dbDefault = "", length = 100, comment = "标题")
  private String title;

  @AutoColumn(name = "content", dbDefault = "", length = 1000, comment = "内容")
  private String content;

  @AutoColumn(name = "rewards", dbDefault = "", length = 200, comment = "奖励")
  private String rewards;

  @AutoColumn(name = "cannot_del", dbDefault = "0", comment = "不能删除 0-能删除 1-不能删除")
  private int cannotDel;

  @AutoColumn(name = "flag", dbDefault = "0", comment = "-1-已删除 0-未读 1-已读 2-已领取")
  private int flag;

  @AutoColumn(name = "expire_time", dbDefault = "0", comment = "过期时间ms")
  private long expireTime;

  @AutoColumn(name = "create_time", dbDefault = "0", comment = "创建时间ms")
  private long createTime;

  @Override
  public Object getPrimaryKey() {
    return playerId + Symbol.UNDERLINE + mailId;
  }

  @Override
  public long getDataId() {
    return playerId;
  }

  @Override
  public int compareTo(DataEntity o) {
    return Long.compare(mailId, ((PlayerMailDO) o).getMailId());
  }
}
