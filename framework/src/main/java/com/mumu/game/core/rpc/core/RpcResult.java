package com.mumu.game.core.rpc.core;

import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.rpcproto.AbstractRpcResponseMessage;
import lombok.Getter;

/**
 * RpcResult
 * 结果返回
 * @author liuzhen
 * @version 1.0.0 2026/6/24 16:03
 */
@Getter
public class RpcResult <V extends AbstractRpcResponseMessage> {

    /** 玩家id（内部使用） */
    private final Long playerId;
    /** 错误码 */
    private final ErrorCode errorCode;
    /** 具体的业务消息通过protoBuf序列化后的二进制数据 */
    private final V rpcResponseMessage;

    public RpcResult(Long playerId, ErrorCode errorCode) {
        this.playerId = playerId;
        this.errorCode = errorCode;
        this.rpcResponseMessage = null;
    }

    public RpcResult(Long playerId, ErrorCode errorCode, V rpcResponseMessage) {
        this.playerId = playerId;
        this.errorCode = errorCode;
        this.rpcResponseMessage = rpcResponseMessage;
    }

    /**
     * 请求是否成功
     * @return boolean
     * @since 2025/6/8 14:24
     */
    public boolean success() {
        return errorCode == ErrorCode.SUCCESS;
    }
}
