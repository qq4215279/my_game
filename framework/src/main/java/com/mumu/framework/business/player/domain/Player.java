/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.business.player.domain;

import com.mumu.framework.business.language.enums.LanguageEnum;
import com.mumu.framework.core.cloud.ServiceType;

import lombok.Data;

/**
 * Player
 * 玩家对象
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:24
 */
@Data
public class Player {
  /** 玩家id */
  private long playerId;

  /** 当前所在服务组 */
  private ServiceType currServerType;

  /** 当前所在服务Id */
  private int currServerId;

  /** 所在服务组 */
  // private Map<ServerGroup, Integer> serverGroups = Maps.newHashMap();

  /** 上次心跳时间（GATE服使用） */
  private long lastHeartbeatTime;

  /** 是否离开 */
  private volatile boolean left;

  /** 离开时间 */
  private long leftTime;

  /** 上线时间 */
  private long onlineTime;

  /** 是否机器人 */
  private boolean robot;

  /** 国际化语言(默认ar) */
  private String languageCode = LanguageEnum.AR.getLanguageCode();

  /** 登陆渠道 */
  private String loginChannel;

  /** 添加玩家所在服务器组 */
  public synchronized void addServerGroup(ServiceType serviceType, int serverId) {
    // 同步更新当前服务位置
    currServerType = serviceType;
    currServerId = serverId;
  }
}
