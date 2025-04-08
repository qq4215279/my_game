/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.cloud;

import com.mumu.common.proto.message.server.ClientServerBean;

import com.mumu.framework.core.mvc.constants.ServiceType;
import lombok.Data;

/**
 * ServerInfo
 * 服务器信息
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:34
 */
@Data
public class ServerInfo {
    /** 服务id(serviceId)，与GameMessageMetadata中的一致 */
    private ServiceType serviceType;
    /** 服务器id */
    private int serverId;
    /** 服务器host */
    private String host;
    /** 服务器端口 */
    private int port;

    /** 构造DTO数据 */
    public ClientServerBean build() {
        ClientServerBean info = new ClientServerBean();
        info.setServiceId(serviceType.getServiceId());
        info.setServerId(serverId);
        info.setIp(host);
        info.setPort(port);
        return info;
    }
}
