package com.mumu.common.mvc.cloud;

import lombok.Data;

/**
 * ServerInfo
 * 服务器信息
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:34
 */
@Data
public class ServerInfo {
    /** 服务id，与GameMessageMetadata中的一致 */
    private int serviceId;
    /** 服务器id */
    private int serverId;
    /** 服务器host */
    private String host;
    /** 服务器端口 */
    private int port;
}
