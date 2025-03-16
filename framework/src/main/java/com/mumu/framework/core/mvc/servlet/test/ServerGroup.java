package com.mumu.framework.core.mvc.servlet.test;

import java.util.List;

/**
 * ServerGroup
 * @author liuzhen
 * @version 1.0.0 2025/3/16 16:30
 */
public enum ServerGroup {
  /** 全部服 */
  ALL(-1),
  /** 未知服 */
  NONE(0),
  /** 注册中心 */
  REGISTER(1),
  /** 网关服 */
  GATE(2),
  /** 大厅服 */
  WORLD(3),
  /** 游戏服 */
  GAME(4),
  /** 聊天服 */
  CHAT(5);

  final int groupId;

  ServerGroup(int groupId) {
    this.groupId = groupId;
  }

  /** 判断是当前服 */
  public boolean inMyself() {
    return curr() == this;
  }

  /** 判断不是当前服 */
  public boolean notMyself() {
    return !inMyself();
  }

  /** 玩家所在的服务组类型，优先级由高到低 */
  public static final List<ServerGroup> PLAYER_ON_GROUPS = List.of(GAME, WORLD, GATE, CHAT);

  /** 游戏相关业务服 */
  public static final List<ServerGroup> GAME_SERVERS = List.of(GAME, WORLD, CHAT);

  /** 获取当前服务类型 */
  public static ServerGroup curr() {
    return CoreConfig.getGroup();
  }
}
