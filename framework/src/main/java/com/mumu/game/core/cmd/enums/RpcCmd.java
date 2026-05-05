package com.mumu.game.core.cmd.enums;

import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.server.ClientServerInfo;
import lombok.Getter;

/**
 * RpcCmd
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 14:31
 */
public enum RpcCmd implements ICmd {
    /** 内部回调 */
    // InnerCallBack,
    /** 握手消息 */
    ServerInfoHandshake(ServiceType.ALL, ClientServerInfo.class, null),

    ;


    /** 消息所属服务id组 */
    @Getter
    private final ServiceType serviceType;
    /** 请求协议消息结构体类型 */
    @Getter
    private final Class<?> reqMsgClass;
    /** 响应协议消息结构体类型 */
    @Getter
    private final Class<?> resMsgClass;

    RpcCmd(ServiceType serviceType, Class<?> reqMsgClass, Class<?> resMsgClass) {
        this.serviceType = serviceType;
        this.reqMsgClass = reqMsgClass;
        this.resMsgClass = resMsgClass;
    }

    @Override
    public boolean isRpc() {
        return true;
    }

}
