package com.mumu.framework.core.group;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.mumu.framework.core.group.impl.DefaultGroupImpl;
import com.mumu.framework.core.group.impl.RedisGroupImpl;
import com.mumu.framework.core.redis.RedisUtil;
import com.mumu.framework.core.redis.constants.RedisKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * GroupManager 组管理器
 *
 * @author liuzhen
 * @version 1.0.0 2024/10/14 22:54
 */
public class GroupManager {

  /** 组Map */
  private static final ConcurrentMap<String, Group> groupMap = new ConcurrentHashMap<>();

  /** 组类型Map */
  private static final ConcurrentMap<GroupTypeEnum, ConcurrentHashSet<String>> groupTypeMap =
      new ConcurrentHashMap<>();

  /** 获取或创建租 */
  protected static Group getOrCreateGroup(GroupTypeEnum type, String groupId) {
    return groupMap.computeIfAbsent(
        groupId,
        id -> {
          Group group = createGroup(type, groupId);
          if (type.isShared()) RedisUtil.sSet(RedisKey.GROUP_ID_KEYS.buildKey(type), groupId);
          else groupTypeMap.computeIfAbsent(type, t -> new ConcurrentHashSet<>()).add(groupId);
          return group;
        });
  }

  /** 创建组 */
  protected static Group createGroup(GroupTypeEnum type, String groupId) {
    return type.isShared()
        ? new RedisGroupImpl(type, groupId)
        : new DefaultGroupImpl(type, groupId);
  }

  /** 查询组（可能为null） */
  public static Group getGroup(String groupId) {
    return groupMap.get(groupId);
  }

  /** 获取某个类型下的全部组ID */
  public static Set<String> getGroupIds(GroupTypeEnum type) {
    if (type.isShared()) return RedisUtil.sGet(RedisKey.GROUP_ID_KEYS.buildKey(type));
    Set<String> set = groupTypeMap.get(type);
    return set == null ? Collections.emptySet() : set;
  }

  /** 获得某个类型下的全部组 */
  public static List<Group> getGroups(GroupTypeEnum type) {
    return getGroupIds(type).stream().map(groupId -> getOrCreateGroup(type, groupId)).toList();
  }

  /** 获取玩家的全部组 */
  public static List<Group> getGroups(long playerId) {
    return GroupTypeEnum.ALL_GROUP_TYPE.stream()
        .flatMap(type -> getGroups(type, playerId).stream())
        .toList();
  }

  /** 获取玩家的某个类型下的全部组 */
  public static List<Group> getGroups(GroupTypeEnum type, long playerId) {
    return getGroups(type).stream().filter(group -> group.hasJoin(playerId)).toList();
  }

  /** 销毁一个组 */
  public static void deleteGroup(GroupTypeEnum type, String groupId) {
    Group remove = groupMap.remove(groupId);
    if (type.isShared()) {
      Optional.ofNullable(remove).orElseGet(() -> createGroup(type, groupId)).clear();
    } else {
      Set<String> groupIds = getGroupIds(type);
      if (!groupIds.isEmpty()) groupIds.remove(groupId);
    }
  }

  /** 销毁某一类型的组 */
  public static void deleteGroup(GroupTypeEnum type) {
    for (String groupId : getGroupIds(type)) {
      deleteGroup(type, groupId);
    }
  }

  /** 玩家离开某个类型下的全部组 */
  public static void leave(GroupTypeEnum type, long playerId) {
    getGroups(type).forEach(group -> group.leave(playerId));
  }

  /** 玩家离开所有组（非自动退出的组不会退出） */
  public static void leave(long playerId) {
    for (GroupTypeEnum type : GroupTypeEnum.ALL_GROUP_TYPE) {
      if (type.isCanAutoLeave()) leave(type, playerId);
    }
  }

  /** 强制玩家离开所有组 */
  public static void forceLeave(long playerId) {
    GroupTypeEnum.ALL_GROUP_TYPE.forEach(type -> leave(type, playerId));
  }
}
