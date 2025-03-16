package com.mumu.framework.core.thread;

import cn.hutool.core.util.StrUtil;

/**
 * ScheduledKey
 * 任务的key
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:55
 */
public enum ScheduledKey {
  /** 玩家延迟下线任务 pid */
  PLAYER_DELAY_OFFLINE("player_delay_offline:{}"),
  /** 比赛轮训任务 */
  TOURNAMENT_LOOP("tournamentLoop"),
  /** 公告推送 noticeId */
  NOTICE_PUSH("notice_push:{}"),
  /** 统计日志刷库任务 */
  STATISTIC_FLUSH("statistic_flush"),
  ;

  final String tmpKey;

  ScheduledKey(String tmpKey) {
    this.tmpKey = tmpKey;
  }

  public String getKey(Object... args) {
    return StrUtil.format(tmpKey, args);
  }
}
