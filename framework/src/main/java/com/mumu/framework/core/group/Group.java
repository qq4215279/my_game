package com.mumu.framework.core.group;

import java.util.Set;

/**
 * Group 组，适用于需要将特定的一些人圈在一个范围里面 这里面的人一个人产生了一个消息，一般需要组里面的人都知晓
 *
 * @author liuzhen
 * @version 1.0.0 @date: 2024/10/14 22:53
 */
public interface Group {

  /**
   * 是否加入当前组
   *
   * @param playerId playerId
   * @return boolean
   * @date 2024/11/5 16:58
   */
  boolean hasJoin(long playerId);

  /**
   * 加入组
   *
   * @param playerId playerId
   * @return boolean
   * @date 2024/11/4 17:31
   */
  boolean join(long playerId);

  /**
   * 离开组
   *
   * @param playerId playerId
   * @return boolean
   * @date 2024/11/4 17:32
   */
  boolean leave(long playerId);

  /**
   * 自动退出
   *
   * @param playerId playerId
   * @return boolean
   * @date 2024/11/4 17:32
   */
  boolean autoLeave(long playerId);

  /**
   * 通知给组内所有人
   *
   * @param cmd cmd
   * @param data data
   * @date 2024/11/4 17:57
   */
  // void notify(Cmd cmd, ErrorCode errorCode, Object data);

  /**
   * 获得组编号
   *
   * @return java.lang.String
   * @date 2024/11/4 17:33
   */
  String getGroupId();

  /** 获取组类型 */
  GroupTypeEnum getGroupType();

  /**
   * 获取组玩家
   *
   * @return java.util.Set<java.lang.Long>
   * @date 2024/11/4 17:51
   */
  Set<Long> getPlayerIdSet();

  /**
   * 清除租中所有玩家
   *
   * @date 2024/11/4 17:54
   */
  void clear();
}
