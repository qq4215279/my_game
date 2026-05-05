package com.mumu.game.core.net.helper;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.ICmd;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.utils.JProtoBufUtil;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

/**
 * GameMessageFactory
 * 游戏详细构建工厂
 * @author liuzhen
 * @version 1.0.0 2026/5/4 09:37
 */
public class GameMessageFactory {
    public static GameMessagePackage reqProxy(ICmd cmd, long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd, true, playerId, errorCode, data);
    }

    public static GameMessagePackage resProxy(ICmd cmd, long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd, false, playerId, errorCode, data);
    }

    /**
     * 创建 GameMessagePackage
     * @param cmd cmd
     * @param req 是否是请求
     * @param playerId 玩家id
     * @param errorCode 错误码
     * @param data 数据报
     * @return com.mumu.game.proto.message.system.message.GameMessagePackage
     */
    private static GameMessagePackage proxy(ICmd cmd, boolean req, long playerId, ErrorCode errorCode, Object data) {
        GameMessagePackage gameMessagePackage = new GameMessagePackage();

        MessageTypeEnum messageType = cmd.isRpc() ? MessageTypeEnum.RPC_REQUEST : MessageTypeEnum.REQUEST;

        GameMessageHeader header = new GameMessageHeader();
        header.setMessageId(cmd.getMessageId());
        header.setMessageType(messageType);

        header = cmd.createGameMessageHeader(req);

        header.setPlayerId(playerId);
        header.setErrorCode(errorCode == null ? ErrorCode.SUCCESS : errorCode);

        // TODO
        header.setSeq(0);
        // TODO 获取当前服务器版本号
        header.setVersion(1000);

        gameMessagePackage.setHeader(header);
        gameMessagePackage.setBody(JProtoBufUtil.encode(data));

        return gameMessagePackage;
    }
}
