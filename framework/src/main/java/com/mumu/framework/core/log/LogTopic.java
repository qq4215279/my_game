package com.mumu.framework.core.log;

import cn.hutool.core.util.ArrayUtil;
import com.mumu.common.constants.SymbolConstants;
import com.mumu.framework.business.player.domain.Player;
import com.mumu.framework.core.log2.LogSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogTopic
 * log 日志主题类别
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:09
 */
public enum LogTopic {
  /** 网络层相关日志 */
  NET,
  /** 通用日志 */
  ACTION,
  /** 缓存模型 */
  MODEL,
  /** 充值相关 */
  CHARGE,
  ;

  /** 普通日志 */
  private static final String PATTERN = "{}";

  /** 玩家维度日志 */
  private static final String PATTERN_PLAYER =
      "{}#playerId#{}#gameId#{}#roomId#{}#tableId#{}#seatId#{}#gold#{}#diamond#{}";

  /** 玩家ID维度日志 */
  private static final String PATTERN_PLAYER_ID = "{}#playerId#{}";

  /** 日志对象 */
  private final Logger log;

  LogTopic() {
    this.log = LoggerFactory.getLogger(this.name());
  }

  /** 异常行为日志 */
  public void error(Throwable e, String action, Object... extras) {
    log.error(getStatisticsKey(PATTERN, extras), action, e);
  }

  /** 异常行为日志 */
  public void error(String action, Object... extras) {
    log.error(getStatisticsKey(PATTERN, extras), action);
  }

  /** 警告行为日志 */
  public void warn(String action, Object... extras) {
    log.warn(getStatisticsKey(PATTERN, extras), action);
  }

  /** 警告行为日志（玩家维度） */
  public void warn(Player player, String action, Object... args) {
    log.warn(
        getStatisticsKey(PATTERN_PLAYER, args),
        action,
        player.getId()
        // player.getGameId(),
        // player.getRoomId(),
        // player.getTableId(),
        // player.getSeatId(),
        // player.getGold(),
        // player.getDiamond()
    );
  }

  /** 警告行为日志（玩家维度） */
  public void warn(long playerId, String action, Object... args) {
    // Player player = PlayerManager.self().getPlayerOrNullable(playerId);
    // TODO
    Player player = new Player();
    if (player != null) warn(player, action, args);
    else log.warn(getStatisticsKey(PATTERN_PLAYER_ID, args), action, playerId);
  }

  /** 普通日志（玩家维度） */
  public void info(long playerId, String action, Object... args) {
    // Player player = PlayerManager.self().getPlayerOrNullable(playerId);
    // TODO
    Player player = new Player();
    if (player != null) {
      info(player, action, args);
    } else {
      log.info(getStatisticsKey(PATTERN_PLAYER_ID, args), action, playerId);
    }
  }

  /** 普通日志（玩家维度） */
  public void info(Player player, String action, Object... args) {
    log.info(
        getStatisticsKey(PATTERN_PLAYER, args),
        action,
        player.getId()
        // player.getGameId(),
        // player.getRoomId(),
        // player.getTableId(),
        // player.getSeatId(),
        // player.getGold(),
        // player.getDiamond()
    );
  }

  /** 普通日志 */
  public void info(String action, Object... args) {
    log.info(getStatisticsKey(PATTERN, args), action);
  }

  /** 普通日志（模块开关控制） */
  // public void debug(LogSwitch switchEnum, String action, Object... args) {
  //   if (switchEnum.getBool()) info(action, args);
  // }

  /** 普通日志（玩家开关控制） */
  public void debug(long playerId, String action, Object... args) {
    // TODO
    // if (LogSwitch.isDebug(playerId)) info(playerId, action, args);
  }

  /** 普通日志（玩家开关控制） */
  public void debug(Player player, String action, Object... args) {
    debug(player.getId(), action, args);
  }

  /** 普通日志（模块开关 || 玩家开关） */
  // public void debug(long playerId, LogSwitch switchEnum, String action, Object... args) {
  //   if (switchEnum.getBool() || LogSwitch.isDebug(playerId)) info(playerId, action, args);
  // }

  /** 普通日志（模块开关 || 玩家开关） */
  // public void debug(Player player, LogSwitch switchEnum, String action, Object... args) {
  //   debug(player.getId(), switchEnum, action, args);
  // }

  /** 拼接日志kv参数 */
  public static String getStatisticsKey(String statistics, Object... extras) {
    return statistics + join(extras);
  }

  public static String join(Object... extras) {
    StringBuilder sb = new StringBuilder();
    for (Object param : extras) {
      sb.append(SymbolConstants.SPLIT_NUMBER)
          .append(ArrayUtil.isArray(param) ? ArrayUtil.toString(param) : param);
    }
    return sb.toString();
  }
}
