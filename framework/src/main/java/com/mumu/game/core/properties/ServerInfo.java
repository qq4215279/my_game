package com.mumu.game.core.properties;

import com.mumu.game.proto.message.server.ClientServerBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mumu.game.core.net.consts.ServerProtocol;
import com.mumu.game.core.net.consts.ServiceType;

import lombok.Data;

/**
 * ServerInfo
 * 服务器配置信息
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:51
 */
@Data
@Component
public class ServerInfo {
    @Value("${application.name}")
    private String serverName;
    @Value("${net.serverId}")
    private int serverId;
    @Value("${net.serviceType}")
    private ServiceType serviceType;
    @Value("${net.version:0}")
    private int version;
    @Value("${net.server.enable:false}")
    private boolean serverEnable;
    @Value("${net.server.protocol:SOCKET}")
    private ServerProtocol protocol;
    @Value("${net.server.ip:127.0.0.1}")
    private String host;
    @Value("${net.server.port:0}")
    private int port;
    @Value("${net.server.master:false}")
    private boolean master;

    /** 构造DTO数据 */
    public ClientServerBean build() {
        // TODO
        ClientServerBean info = new ClientServerBean();
        info.setServiceId(serviceType.getServiceId());
        info.setServerId(serverId);
        info.setIp(host);
        info.setPort(port);
        return info;
    }
}

