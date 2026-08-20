package com.mumu.game.core.game;

import com.mumu.game.core.cmd.response.ResponseResult;

import java.util.List;

/**
 * FruitService
 * 水果机service
 * @author liuzhen
 * @version 1.0.0 2025/6/19 15:28
 */
public interface FruitService {

  /**
   * 请求执行水果机游戏命令
   * @param playerId playerId
   * @param command command
   * @param args args
   * @return com.game.framework.core.cmd.response.ResponseResult
   * @since 2025/6/19 18:27
   */
  void executeCommand(long playerId, FruitCommandEnum command, List<String> args);

  /**
   * 请求获取近20场水果机金币排行玩家信息
   * @param playerId playerId
   * @param type 类型 0: 在线榜; 1: 单局榜
   * @param start start
   * @param end end
   * @param version version
   * @return com.game.framework.core.cmd.response.ResponseResult
   * @since 2025/6/19 18:29
   */
  ResponseResult getFruitRecentGoldRankList(long playerId, int type, int start, int end, long version);
}
