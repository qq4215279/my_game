package com.mumu.game.core.net.helper;

import com.mumu.game.core.cmd.enums.Cmd;
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

    public static GameMessagePackage reqProxy(
            Cmd cmd, ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd.getReqMessageId(), MessageTypeEnum.REQUEST, toServiceType, toServerId, playerId, errorCode, data);
    }

    public static GameMessagePackage resProxy(
            Cmd cmd, ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        return proxy(cmd.getResMessageId(), MessageTypeEnum.RESPONSE, toServiceType, toServerId, playerId, errorCode,
                data);
    }

    /**
     * 创建 GameMessagePackage
     * @param messageId 协议id
     * @param messageType 消息类型
     * @param toServiceType to服务id组
     * @param toServerId to服务器id
     * @param playerId 玩家id
     * @param errorCode 错误码
     * @param data data
     * @return com.mumu.common.proto.message.system.message.GameMessagePackage
     */
    private static GameMessagePackage proxy(
            int messageId, MessageTypeEnum messageType,
            ServiceType toServiceType, int toServerId,
            long playerId, ErrorCode errorCode, Object data) {
        GameMessagePackage gameMessagePackage = new GameMessagePackage();

        GameMessageHeader header = new GameMessageHeader();
        header.setMessageId(messageId);
        header.setMessageType(messageType);

        // TODO 设置当前服务器信息
        // header.setFromServerId(1);
        // header.setFromServerId(1);

        // header.setToServiceId(toServiceType.getServiceId());
        // header.setToServerId(toServerId);
        header.setPlayerId(playerId);
        header.setErrorCode(errorCode == null ? ErrorCode.SUCCESS : errorCode);

        // TODO
        header.setSeq(1000000);
        // TODO 获取客户端发送时间
        long now = System.currentTimeMillis();
        header.setSendTime(now);
        // TODO 获取当前服务器版本号
        header.setVersion(1000);

        gameMessagePackage.setHeader(header);
        gameMessagePackage.setBody(JProtoBufUtil.encode(data));

        return gameMessagePackage;
    }
}
