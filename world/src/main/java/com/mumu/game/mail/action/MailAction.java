package com.mumu.game.mail.action;

import com.game.business.player.manager.PlayerManager;
import com.game.framework.core.cmd.anno.CmdAction;
import com.game.framework.core.cmd.anno.CmdMapping;
import com.game.framework.core.cmd.consts.Cmd;
import com.game.framework.core.log.LogTopic;
import com.game.framework.net.consts.ServerGroup;
import com.game.framework.net.server.MessageContext;
import com.game.framework.net.server.MessageSender;
import com.game.proto.core.ErrorCode;
import com.game.proto.mail.CWOperateMailMessage;
import com.game.proto.mail.WCOperateMailMessage;
import com.game.template.func.core.FunctionIdEnum;
import com.game.template.func.mail.MailTemplate;

import jakarta.annotation.Resource;

/** 邮件操作处理类 @Date: 2024/9/2 下午4:58 @Author: xu.hai */
@CmdAction
public class MailAction {
  @Resource PlayerManager playerManager;

  /** 玩家操作邮件 */
  @CmdMapping(Cmd.CWOperateMail)
  public void operateMail(MessageContext context) {
    long playerId = context.getPlayerId();
    CWOperateMailMessage msg = context.getMsg(CWOperateMailMessage.class);
    try {
      if (checkPlayer(context)) return;
      // 邮件模版
      MailTemplate template = FunctionIdEnum.WORLD_MAIL.loadFuncTemplate(playerId, MailTemplate.class);
      if (!template.isOpen(playerId)) {
        MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_FUNCTION_NOT_OPOEN, Cmd.WCOperateMail);
        return;
      }

      WCOperateMailMessage resMsg = new WCOperateMailMessage();
      ErrorCode errorCode = template.execute(playerId, msg, resMsg);
      // 返回消息
      if (errorCode != ErrorCode.SUCCESS) MessageSender.sendToPlayer(playerId, errorCode);
      else MessageSender.sendToPlayer(playerId, Cmd.WCOperateMail, resMsg);
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "operateMail", "playerId", playerId, "msg", msg);
      MessageSender.sendToPlayer(playerId, ErrorCode.FAIL);
    }
  }

  /** 前置检查玩家位置 */
  private boolean checkPlayer(MessageContext context) {
    long playerId = context.getPlayerId();
    if (playerManager.inServer(playerId)) return false;

    // 路由到玩家所在服处理，或者返回给客户端
    int serverId = playerManager.getServerId(playerId, ServerGroup.WORLD);
    if (serverId != 0) MessageSender.sendToServer(serverId, context.getProxy());
    else MessageSender.sendToPlayer(playerId, ErrorCode.FAIL_PLAYER_OFFLINE);
    return true;
  }
}
