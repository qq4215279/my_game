package com.mumu.game.core.game;

import java.util.List;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.proto.message.core.ErrorCode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


import cn.hutool.core.lang.Pair;
import jakarta.annotation.Resource;

/**
 * FruitServiceImpl
 * 水果机service
 * @author liuzhen
 * @version 1.0.0 2025/6/19 15:40
 */
@Service
public class FruitServiceImpl implements FruitService {
  /** log */
  private static final LogTopic log = LogTopic.ACTION;

  /** 数量 */
  private static final int COUNT = 10;

  // @Resource
  // private FruitMachine fruitMachine;
  // @Resource
  // private PlayerFruitDOManager playerFruitDOManager;

  /** 玩家下线事件 */
  /*@EventListener(OfflineEvent.class)
  public void onOffline(OfflineEvent event) {
    long playerId = event.getPlayer().getPlayerId();
    FruitPlayer fruitPlayer = fruitMachine.getFruitPlayer(playerId);
    if (fruitPlayer == null) {
      return;
    }

    // 退出游戏
    fruitMachine.exitAction(true, playerId);
  }*/


  @Override
  public void executeCommand(long playerId, FruitCommandEnum cmd, List<String> args) {
    if (cmd == null) {
      // 返回失败消息
      // MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_COMMAND_NOT_EXIST);
      return;
    }

    // Command command = fruitMachine.getCommand(cmd);
    Command command = null;
    if (!(command instanceof AbstractFruitPlayerCommand playerCommand)) {
      // MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_COMMAND_NOT_EXIST);
      return;
    }

    // 命令校验
    Pair<Boolean, ErrorCode> pari = playerCommand.checkExecute(playerId, args);
    ErrorCode errorCode = pari.getValue();
    if (!pari.getKey()) {
      // MessageSender.sendToPlayer(playerId, errorCode);
      return;
    }

    // 先返回，在执行命令
    // MessageSender.sendToPlayer(playerId, Cmd.ZCFruitExecuteCommand, new ZCFruitExecuteCommandMessage());

    // 执行操作
    playerCommand.execute(playerId, args);
  }

  @Override
  public ResponseResult getFruitRecentGoldRankList(long playerId, int type, int start, int end,
                                                   long version) {
    // long nowVersion = fruitMachine.getRound();
    long nowVersion = 0;
    if (version != nowVersion) {
      start = 1;
      end = COUNT;
    }

    if (end <= start) {
      end = start + COUNT;
    }

    // ZCGetFruitRecentGoldRankListMessage resMsg = new ZCGetFruitRecentGoldRankListMessage();
    // RankTypeEnum rankTypeEnum = getRankTypeEnum(type);
    // Pair<Integer, Long> pair = rankTypeEnum.getMyRankAndScore(playerId);
    // resMsg.setSelfPlayerBetRankBean(buildPlayerBetRankBean(playerId, fruitMachine, playerId, pair.getValue(), pair.getKey()));
    //
    // // 排行榜人数
    // List<RankData> rankList = rankTypeEnum.getRankList(start, end);
    // int rank = start;
    // for (RankData rankData : rankList) {
    //   resMsg.getPlayerBetRankBeanList().add(buildPlayerBetRankBean(playerId, fruitMachine, rankData.vid, rankData.score, rank++));
    // }
    //
    // resMsg.setVersion(nowVersion);
    // resMsg.setPlayerCount(fruitMachine.getPlayerCount());

    // return ResponseResult.success(playerId, Cmd.ZCGetFruitRecentGoldRankList, resMsg);
    return ResponseResult.success(playerId);
  }

  /**
   * 获取排行榜类型
   * @param type 类型 0: 在线榜; 1: 单局榜
   * @return com.game.framework.core.rank.RankTypeEnum
   * @since 2025/6/25 14:37
   */
  // private static RankTypeEnum getRankTypeEnum(int type) {
  //   // 在线榜
  //   if (type == 0) {
  //     return RankTypeEnum.FRUIT_ONLINE_GOLD;
  //
  //     // 单局榜
  //   } else {
  //     return RankTypeEnum.FRUIT_SINGLE_ROUND_GOLD;
  //   }
  // }

  /**
   * 构建水果机小游戏玩家排行信息
   * @param fruitMachine fruitMachine
   * @param targetPlayerId playerId
   * @param rank rank
   * @return com.game.proto.bet.PlayerBetRankBean
   * @since 2025/6/24 13:58
   */
  // private PlayerBetRankBean buildPlayerBetRankBean(long playerId, FruitMachine fruitMachine, long targetPlayerId, long score, int rank) {
  //   PlayerBetRankBean playerBetRankBean = new PlayerBetRankBean();
  //
  //   long round = fruitMachine.getRound();
  //   boolean self = targetPlayerId == playerId;
  //   playerBetRankBean.setWinCount(playerFruitDOManager.getWinNum(targetPlayerId, round, self));
  //   long winGold = playerFruitDOManager.getWinGold(targetPlayerId, round, self);
  //   playerBetRankBean.setGoldChangeNum(winGold);
  //   playerBetRankBean.setPlayerRankBean(createPlayerRank(targetPlayerId, score, rank));
  //   return playerBetRankBean;
  // }

  /** 构建水果机玩家排行榜信息 */
  // private static PlayerRankBean createPlayerRank(long playerId, long score, int rank) {
  //   PlayerRankBean rankBean = new PlayerRankBean();
  //   rankBean.setId(playerId);
  //   rankBean.setScore(score);
  //   rankBean.setRank(rank > RankConstants.RANK_LIMIT ? -1 : rank);
  //
  //   // 获取通用游戏玩家信息
  //   PlayerSimpleBean baseInfo = PlayerUtil.buildSimpleBean(playerId);
  //   rankBean.setNick(baseInfo.getNick());
  //   rankBean.setHead(baseInfo.getHead());
  //   rankBean.setHeadFrame(baseInfo.getHeadFrame());
  //   rankBean.setLevel(baseInfo.getLevel());
  //   rankBean.setVip(baseInfo.getVip());
  //   rankBean.setHasVip(baseInfo.getHasVip());
  //
  //   return rankBean;
  // }
}
