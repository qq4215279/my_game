package com.mumu.game.mail.operator;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.game.business.mail.domain.PlayerMailIdxDO;
import com.game.framework.core.automodel.entity.IDataOperator;

/** 系统邮件数据访问类 @Date: 2024/9/3 下午2:56 @Author: xu.hai */
@Component
public class WorldMailIdxOperator implements IDataOperator<PlayerMailIdxDO> {

  /** 获取玩家邮件索引DO */
  public PlayerMailIdxDO getMailIdxDO(long playerId) {
    return selectOrNew(
        () -> {
          PlayerMailIdxDO mailIdxDO = new PlayerMailIdxDO();
          mailIdxDO.setPlayerId(playerId);
          return mailIdxDO;
        },
        playerId,
        playerId);
  }

  /** 获取系统邮件索引 */
  public long getSysMailIdx(long playerId) {
    return Optional.ofNullable(selectOne(playerId, playerId))
        .map(PlayerMailIdxDO::getSysMailIdx)
        .orElse(0L);
  }

  /** 更新系统邮件获取索引 */
  public void updateSysMailIdx(long playerId, long sysMailIdx) {
    PlayerMailIdxDO mailIdxDO = getMailIdxDO(playerId);
    mailIdxDO.setSysMailIdx(sysMailIdx);
    update(playerId, mailIdxDO);
  }
}
