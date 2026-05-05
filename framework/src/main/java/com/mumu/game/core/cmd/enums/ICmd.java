package com.mumu.game.core.cmd.enums;

import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.MessageTypeEnum;

/**
 * ICmd
 * @author liuzhen
 * @version 1.0.0 2026/5/5 14:55
 */
public interface ICmd {

    /**
     * 获取消息所属服务id组
     * @return com.mumu.game.core.net.consts.ServiceType
     */
    ServiceType getServiceType();

    /**
     * 获取请求协议消息结构体类型
     * @return java.lang.Class<?>
     */
    Class<?> getReqMsgClass();

    /**
     * 获取响应协议消息结构体类型
     * @return java.lang.Class<?>
     */
    Class<?> getResMsgClass();

    /**
     * 获取请求Cmd对应messageId
     * @return int
     */
    default int getMessageId() {
        return CmdManager.getMessageId(this);
    }

    /**
     * 构建GameMessageHeader
     * @param req 是不是请求
     * @return com.mumu.game.proto.message.system.message.GameMessageHeader
     */
    default GameMessageHeader createGameMessageHeader(boolean req) {
        GameMessageHeader header = new GameMessageHeader();

        header.setMessageId(getMessageId());
        if (!isRpc()) {
            header.setMessageType(req ? MessageTypeEnum.REQUEST : MessageTypeEnum.RESPONSE);
        } else {
            header.setMessageType(req ? MessageTypeEnum.RPC_REQUEST : MessageTypeEnum.RPC_RESPONSE);
        }
        header.setSendTime(System.currentTimeMillis());

        return header;
    }

    /**
     * 拷贝消息头
     * @param header 消息头
     * @return 拷贝的消息头
     */
    default GameMessageHeader clone(GameMessageHeader header) {
        GameMessageHeader headerClone = new GameMessageHeader();
        headerClone.setMessageId(header.getMessageId());
        headerClone.setMessageType(header.getMessageType());
        headerClone.setSeq(header.getSeq());
        headerClone.setSendTime(header.getSendTime());
        headerClone.setVersion(header.getVersion());
        headerClone.setPlayerId(header.getPlayerId());
        headerClone.setErrorCode(header.getErrorCode());
        return headerClone;
    }

    /**
     * 是否是rpc cmd
     */
    default boolean isRpc() {
        return false;
    }
}
