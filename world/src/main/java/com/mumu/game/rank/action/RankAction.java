package com.mumu.game.rank.action;

import java.util.Map;

import com.game.business.player.common.PlayerAttributeEnum;
import com.game.business.rank.enums.RankFunc;
import com.game.business.rank.enums.RankType;
import com.game.framework.core.cmd.anno.CmdAction;
import com.game.framework.core.cmd.anno.CmdMapping;
import com.game.framework.core.cmd.consts.Cmd;
import com.game.framework.core.cmd.response.ResponseResult;
import com.game.framework.net.server.MessageContext;
import com.game.proto.rank.CWGetRankInfosMessage;
import com.game.proto.rank.CWGetRankScoreMessage;
import com.game.proto.rank.RankScoreBean;
import com.game.proto.rank.WCGetRankInfosMessage;
import com.game.proto.rank.WCGetRankScoreMessage;
import com.game.rank.service.RankService;

import cn.hutool.core.util.EnumUtil;
import jakarta.annotation.Resource;

/** 排行榜请求接口 @Date: 2024/11/29 下午2:35 @Author: xu.hai */
@CmdAction
public class RankAction {

  @Resource RankService rankService;

  /** 请求排行榜信息 */
  @CmdMapping(Cmd.CWGetRankInfos)
  public ResponseResult getRankInfos(MessageContext context) {
    CWGetRankInfosMessage msg = context.getMsg(CWGetRankInfosMessage.class);
    RankFunc func = RankFunc.getRankFunc(msg.getFunctionId());
    RankType type = EnumUtil.getEnumAt(RankType.class, msg.getType().ordinal());

    long playerId = context.getPlayerId();
    if (func == null || type == null || func.getType(type) == null) {
      return ResponseResult.errorByParam(playerId, Cmd.WCGetRankInfos);
    }

    // 版本号不一致时，offset置0
    int sysVersion = rankService.getVersion();
    int offset = msg.getVersion() == sysVersion ? msg.getOffset() : 0;

    WCGetRankInfosMessage resMsg = new WCGetRankInfosMessage();
    resMsg.setRankBeans(rankService.getRankInfos(func, type, msg.getPeriod(), offset));
    resMsg.setOwn(rankService.getRankInfo(playerId, func, type, msg.getPeriod()));
    resMsg.setVersion(sysVersion);
    return ResponseResult.success(playerId, Cmd.WCGetRankInfos, resMsg);
  }

  /** 请求排行榜积分信息 */
  @CmdMapping(Cmd.CWGetRankScore)
  public ResponseResult getRankScore(MessageContext context) {
    CWGetRankScoreMessage msg = context.getMsg();
    RankFunc func = RankFunc.getRankFunc(msg.getFunctionId());
    RankType type = EnumUtil.getEnumAt(RankType.class, msg.getType().ordinal());

    long playerId = context.getPlayerId();
    if (func == null || type == null || func.getType(type) == null) {
      return ResponseResult.errorByParam(playerId, Cmd.WCGetRankScore);
    }
    Object[] params = msg.getParams().toArray();
    Map<Long, RankScoreBean> topN = func.getTopN(type, msg.getPeriod(), params);
    // 获取自己的排名
    long id = playerId;
    // 语音房排名，需要获取自己的语音房id
    if (func == RankFunc.VOICE_TABLE_GLOBAL) id = PlayerAttributeEnum.VOICE_ID.getInt(playerId);
    RankScoreBean ownerBean = func.getScoreInfo(id, type, msg.getPeriod(), params);

    WCGetRankScoreMessage resMsg = new WCGetRankScoreMessage();
    resMsg.getRankBeans().addAll(topN.values());
    resMsg.setOwn(ownerBean);
    return ResponseResult.success(playerId, Cmd.WCGetRankScore, resMsg);
  }
}
