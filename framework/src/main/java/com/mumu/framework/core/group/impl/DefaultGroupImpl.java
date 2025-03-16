package com.mumu.framework.core.group.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.mumu.framework.core.group.GroupTypeEnum;
import java.util.Set;

/**
 * DefaultGroupImpl 默认组实现类
 *
 * @author liuzhen
 * @version 1.0.0 2024/10/14 22:54
 */
public class DefaultGroupImpl extends AbstractGroup {
  /** 组中成员Map */
  private final Set<Long> playerIdSet = new ConcurrentHashSet<>();

  public DefaultGroupImpl(GroupTypeEnum groupType, String groupId) {
    super(groupType, groupId);
  }

  @Override
  public boolean hasJoin(long playerId) {
    return playerIdSet.contains(playerId);
  }

  @Override
  public boolean join(long playerId) {
    return playerIdSet.add(playerId);
  }

  @Override
  public boolean leave(long playerId) {
    return playerIdSet.remove(playerId);
  }

  @Override
  public Set<Long> getPlayerIdSet() {
    return playerIdSet;
  }

  @Override
  public void clear() {
    playerIdSet.clear();
  }
}
