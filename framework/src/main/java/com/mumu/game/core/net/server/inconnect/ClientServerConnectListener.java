/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.server.inconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.register.bo.RegisteredServerInfo;
import com.mumu.game.proto.message.server.ClientServerBean;
import com.mumu.game.proto.server.ClientServerInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.core.register.ServiceRegistryListener;
import com.mumu.game.core.register.bo.ServiceRegistrySnapshot;

/**
 * ClientServerConnectListener
 * 将注册中心全量目录同步为 Netty 客户端连接：除本进程自身外，对目录中所有类型的实例发起/维持连接；同机同类型的其它 serverId 也会连接
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
@ConditionalOnBean(ClientServer.class)
@ConditionalOnProperty(prefix = "net.client", name = "enable", havingValue = "true")
public class ClientServerConnectListener implements ServiceRegistryListener {

    private final ClientServer clientServer;

    /** 本机配置，用于排除「连自己」 */
    private final ServerInfo localServerInfo;

    public ClientServerConnectListener(ClientServer clientServer, ServerInfo serverInfo) {
        this.clientServer = clientServer;
        this.localServerInfo = serverInfo;
    }

    @Override
    public void onSnapshotUpdated(ServiceRegistrySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        Map<ServiceType, List<ClientServerBean>> remoteMap = buildRemoteServersMap(snapshot);
        clientServer.connectServers(remoteMap);
    }

    /**
     * 为每一种真实服务类型构造远端列表（无实例则为空列表），以便 {@link ClientServer#connectServers(java.util.Map)} 断开已从目录消失的类型实例
     */
    private Map<ServiceType, List<ClientServerBean>> buildRemoteServersMap(ServiceRegistrySnapshot snapshot) {
        Map<Integer, Map<String, RegisteredServerInfo>> byTypeId = snapshot.getServiceMapByClone();
        Map<ServiceType, List<ClientServerBean>> out = new LinkedHashMap<>();
        for (ServiceType st : ServiceType.values()) {
            if (st == ServiceType.ALL || st == ServiceType.NONE) {
                continue;
            }
            List<ClientServerBean> list = new ArrayList<>();
            Map<String, RegisteredServerInfo> row = byTypeId.get(st.getServiceId());
            if (row != null) {
                for (RegisteredServerInfo rec : row.values()) {
                    if (rec == null) {
                        continue;
                    }
                    ServiceType rowType = ServiceType.getServiceType(rec.getServiceTypeId());
                    if (rowType == null) {
                        LogTopic.NET.error("registryConnect.skipUnknownType", "serviceTypeId", rec.getServiceTypeId(), "serverId",
                            rec.getServerId());
                        continue;
                    }
                    if (rowType != st) {
                        LogTopic.NET.error("registryConnect.typeMismatch", "expected", st, "recordTypeId", rec.getServiceTypeId());
                        continue;
                    }
                    // 仅排除本机自身；同类型其它 serverId（含同机多进程）照常连接
                    if (isLocalInstance(rec)) {
                        continue;
                    }
                    list.add(toRemoteServerInfo(rec, st));
                }
            }
            out.put(st, list);
        }
        return out;
    }

    private boolean isLocalInstance(RegisteredServerInfo rec) {
        return rec.getServiceTypeId() == localServerInfo.getServiceType().getServiceId()
            && rec.getServerId() == localServerInfo.getServerId();
    }

    /**
     * 由注册信息构造用于建连的 ClientServerInfo；{@link ClientServer} 使用 host 字段作为连接地址，故优先填 ip
     */
    private ClientServerBean toRemoteServerInfo(RegisteredServerInfo r, ServiceType serviceType) {
        ClientServerBean clientServerInfo = new ClientServerBean();
        clientServerInfo.setServiceId(serviceType.getServiceId());
        clientServerInfo.setServerId(r.getServerId());
        String dial = StringUtils.isNotBlank(r.getIp()) ? r.getIp() : r.getHost();
        if (StringUtils.isBlank(dial)) {
            dial = "127.0.0.1";
        }
        clientServerInfo.setIp(dial);
        clientServerInfo.setPort(r.getPort());

        return clientServerInfo;
    }
}
