package com.mumu.framework.core.group.impl;

import com.mumu.framework.core.group.GroupTypeEnum;
import com.mumu.framework.core.redis.RedisUtil;
import com.mumu.framework.core.redis.constants.RedisKey;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RedisGroupImpl Redis组实现类
 *
 * @author liuzhen
 * @version 1.0.0 2024/11/5 15:42
 */
public class RedisGroupImpl extends AbstractGroup {

  public RedisGroupImpl(GroupTypeEnum groupType, String groupId) {
    super(groupType, groupId);
  }

  /** 获取Redis中记录组成员的Key */
  protected String getRedisGroupMemberKey() {
    return RedisKey.GROUP_MEMBER_KEY.buildKey(groupId);
  }

  /** 获取Redis中记录组类型ID的Key */
  protected String getRedisGroupIdKey() {
    return RedisKey.GROUP_ID_KEYS.buildKey(groupType);
  }

  @Override
  public boolean hasJoin(long playerId) {
    return RedisUtil.sHasKey(getRedisGroupMemberKey(), playerId);
  }

  @Override
  public boolean join(long playerId) {
    return RedisUtil.sSet(getRedisGroupMemberKey(), String.valueOf(playerId)) > 0;
  }

  @Override
  public boolean leave(long playerId) {
    return RedisUtil.setRemove(getRedisGroupMemberKey(), String.valueOf(playerId)) > 0;
  }

  @Override
  public Set<Long> getPlayerIdSet() {
    Set<String> playerIdSet = RedisUtil.sGet(getRedisGroupMemberKey());
    return playerIdSet.stream().map(Long::parseLong).collect(Collectors.toSet());
  }

  @Override
  public void clear() {
    RedisUtil.del(getRedisGroupMemberKey());
    RedisUtil.setRemove(getRedisGroupIdKey(), groupId);
  }
}
