/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register.bo;

import com.mumu.game.core.properties.ServerInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RegisteredServerInfo
 * 注册到 Redis 的单条服务器信息，对应按 serverType 分组的 Hash 中一条 field（field 名即 serverId）
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredServerInfo {

    /**
     * 实例唯一标识
     * 与 Redis 中按类型划分的 Hash 的 field 名称一致，建议在集群内唯一（如逻辑 serverId、UUID 等）
     */
    private int serverId;

    /**
     * 服务类型编号
     * 取值与 {@link com.mumu.game.core.net.consts.ServiceType#getServiceId()} 一致，用于分桶存储与路由
     */
    private int serviceTypeId;

    /**
     * 对外访问主机名或域名
     * 客户端或其它服务用于建连展示或解析的 host
     */
    private String host;

    /**
     * IP 地址
     * 可与 host 相同；若仅使用 IP 注册，可与 host 填同一值
     */
    private String ip;

    /**
     * 对外服务端口
     * 与其它服务建立 Netty/TCP 等连接时使用的端口
     */
    private int port;

    public static RegisteredServerInfo of(ServerInfo serverInfo) {
        RegisteredServerInfo info = new RegisteredServerInfo();
        info.serverId = serverInfo.getServerId();
        info.serviceTypeId = serverInfo.getServiceType().getServiceId();
        info.host = serverInfo.getHost();
        info.ip = serverInfo.getHost();
        info.port = serverInfo.getPort();
        return info;
    }
}
