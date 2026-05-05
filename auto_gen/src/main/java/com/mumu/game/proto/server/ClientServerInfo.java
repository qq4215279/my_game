package com.mumu.game.proto.server;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * ClientServerInfo
 * 服务器信息 - 用于数据传递
 * @author liuzhen
 * @version 1.0.0 2026/5/5 14:37
 */
@Data
@ProtobufClass
public class ClientServerInfo {
    /** 服务器名称 */
    private String serverName;

    /** 服务器组类型 */
    private int serviceId;

    /** 服务器编号 */
    private int serverId;

    /** 服务器版本号 */
    private int serverVersion;

    /** 服务端是否开启 */
    private boolean serverEnable;

    /** 服务端协议 */
    private String protocol;

    /** 服务器ip */
    private String ip;

    /** 服务器端口 */
    private Integer port;

    /** 是否是主服务 */
    private boolean master;
}
