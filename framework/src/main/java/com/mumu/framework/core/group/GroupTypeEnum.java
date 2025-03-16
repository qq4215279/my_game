package com.mumu.framework.core.group;

import com.mumu.common.constants.SymbolConstants;
import java.util.List;
import lombok.Getter;

/**
 * 组类型枚举
 *
 * @author liuzhen
 * @version 1.0.0 2024/11/5 15:27
 */
@Getter
public enum GroupTypeEnum {
  /** baloot自建房列表 */
  BALOOT_TABLE_LIST,
  /** baloot自建房聊天分享 */
  BALOOT_TABLE_CHAT,
  /** 工会组 */
  CLUB,
  /** 小游戏 - 下注组 */
  MINI_GAME_BET,
  ;

  public static final List<GroupTypeEnum> ALL_GROUP_TYPE = List.of(values());

  /** 组缓存能否多服务器共享 */
  private final boolean shared;

  /** 是否可以自动退出 */
  private final boolean canAutoLeave;

  GroupTypeEnum() {
    this(false, true);
  }

  GroupTypeEnum(boolean shared, boolean canAutoLeave) {
    this.shared = shared;
    this.canAutoLeave = canAutoLeave;
  }

  /** 获取组id */
  public String getGroupId(Object... params) {
    StringBuilder key = new StringBuilder(name());
    for (Object suffix : params) {
      key.append(SymbolConstants.UNDERLINE).append(suffix);
    }
    return key.toString();
  }

  /** 获取分组（没有则创建） */
  public Group getGroup(Object... params) {
    return GroupManager.getOrCreateGroup(this, getGroupId(params));
  }

  /** 移除分组 */
  public void remove(Object... params) {
    GroupManager.deleteGroup(this, getGroupId(params));
  }
}
