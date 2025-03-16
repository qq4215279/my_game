package com.mumu.framework.core.group.impl;

import com.alibaba.druid.wall.violation.ErrorCode;
import com.mumu.framework.core.group.Group;
import com.mumu.framework.core.group.GroupTypeEnum;
import lombok.Getter;

/**
 * AbstractGroup 抽象组
 *
 * @author liuzhen
 * @version 1.0.0 2024/11/5 15:56
 */
@Getter
public abstract class AbstractGroup implements Group {
  /** 组类型 */
  protected final GroupTypeEnum groupType;

  /** 组Id */
  protected final String groupId;

  public AbstractGroup(GroupTypeEnum groupType, String groupId) {
    this.groupType = groupType;
    this.groupId = groupId;
  }

  @Override
  public boolean autoLeave(long playerId) {
    return groupType.isCanAutoLeave() && leave(playerId);
  }

  // @Override
  // public void notify(Cmd cmd, ErrorCode errorCode, Object data) {
  //   MessageSender.broadcastIncludeIds(cmd, errorCode, data, getPlayerIdSet());
  // }
}
