package com.mumu.game.core.net.helper;

import com.mumu.game.core.net.server.IoSession;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.core.net.session.SessionManager;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

/**
 * MessageSender
 * 消息发送器
 * @author liuzhen
 * @version 1.0.0 2026/5/4 09:25
 */
public final class MessageSender {

    public static boolean send(MessageContext context) {
        return send(context.getSession(), context.getMessagePackage());
    }

    /** 向指定 IoSession 发送数据 */
    public static boolean send(IoSession session, GameMessagePackage gameMessagePackage) {
        MessageContext context = MessageContext.of(gameMessagePackage, session);
        if (session == null) {
            context.error("send session is null");
            return false;
        }
        session.write(gameMessagePackage);

        // 打日志
        GameMessageHeader header = gameMessagePackage.getHeader();
        MessageTypeEnum messageType = header.getMessageType();
        if (messageType == MessageTypeEnum.RPC_REQUEST || messageType == MessageTypeEnum.RPC_RESPONSE) {
            context.debug("rpcResponse");
        } else {
            context.debug("send");
        }
        return true;
    }

    /** 向玩家发送消息（proxy中必须指定玩家id） */
    public static void sendToPlayer(GameMessagePackage messagePackage) {
        GameMessageHeader header = messagePackage.getHeader();
        Long playerId = header.getPlayerId();
        if (playerId == null) {
            MessageContext.of(messagePackage, null).error("send playerId is null");
            return;
        }

        if (!PlayerManager.self().isRobot(playerId)) {
            send(SessionManager.self().getOutSession(playerId), messagePackage);
        }
    }

}
