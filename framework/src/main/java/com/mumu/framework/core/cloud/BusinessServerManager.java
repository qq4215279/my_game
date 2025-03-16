package com.mumu.framework.core.cloud;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BusinessServerManager
 * 业务服务管理器
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:32
 */
@Component
public class BusinessServerManager implements ApplicationListener<HeartbeatEvent> {
    @Resource
    private DiscoveryClient discoveryClient;

    /** serviceId 对应的服务器集合，一个服务可能部署到多台服务器上面，实现负载均衡 */
    private Map<Integer, List<ServerInfo>> serverInfoMap;

    @PostConstruct
    public void init() {
        this.refreshBusinessServerInfo();
    }

    @Override
    public void onApplicationEvent(HeartbeatEvent event) {
        this.refreshBusinessServerInfo();
    }

    /**
     * 刷新网关后面的服务列表
     * @return void
     * @date 2024/6/19 11:24
     */
    private void refreshBusinessServerInfo() {
        Map<Integer, List<ServerInfo>> tempServerInfoMap = new HashMap<>();
        // 网取网关后面的服务实例
        List<ServiceInstance> businessServiceInstances = discoveryClient.getInstances("game-logic");
//        logger.debug("抓取游戏服务配置成功,{}", businessServiceInstances);

        businessServiceInstances.forEach(instance -> {
            int weight = this.getServerInfoWeight(instance);
            for (int i = 0; i < weight; i++) {
                ServerInfo serverInfo = this.createServerInfo(instance);
                tempServerInfoMap.computeIfAbsent(serverInfo.getServiceId(), k -> new ArrayList<>()).add(serverInfo);
            }
        });

        this.serverInfoMap = tempServerInfoMap;
    }

    private int getServerInfoWeight(ServiceInstance instance) {
        String value = instance.getMetadata().get("weight");
        if (value == null) {
            value = "1";
        }
        return Integer.parseInt(value);
    }

    private ServerInfo createServerInfo(ServiceInstance instance) {
        String serviceId = instance.getMetadata().get("serviceId");
        String serverId = instance.getMetadata().get("serverId");
        if (StringUtils.isEmpty(serviceId)) {
            throw new IllegalArgumentException(instance.getHost() + "的服务未配置serviceId");
        }

        if (StringUtils.isEmpty(serverId)) {
            throw new IllegalArgumentException(instance.getHost() + "的服务未配置serverId");
        }

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setServiceId(Integer.parseInt(serviceId));
        serverInfo.setServerId(Integer.parseInt(serverId));
        serverInfo.setHost(instance.getHost());
        serverInfo.setPort(instance.getPort());

        return serverInfo;
    }

    /**
     * 从游戏网关列表中选择一个游戏服务实例信息返回。
     * @param serviceId serviceId
     * @param playerId playerId
     * @return com.mygame.common.model.ServerInfo
     * @date 2024/6/19 11:24
     */
    public ServerInfo selectServerInfo(int serviceId, Long playerId) {
        // 再次声明一下，防止游戏网关列表发生变化，导致数据不一致。
        Map<Integer, List<ServerInfo>> serverInfoMap = this.serverInfoMap;
        List<ServerInfo> serverList = serverInfoMap.getOrDefault(serviceId, Collections.emptyList());
        if (serverList.isEmpty()) {
            return null;
        }

        int hashCode = Math.abs(playerId.hashCode());
        int gatewayCount = serverList.size();
        int index = hashCode % gatewayCount;
        if (index >= gatewayCount) {
            index = gatewayCount - 1;
        }
        return serverList.get(index);
    }

    /**
     * 判断某个服务中的serverId是否还有效
     * Description:
     * @param serviceId
     * @param serverId
     * @return
     * @author wgs
     * @date 2019年5月18日 下午6:20:59
     *
     */
    public boolean isEnableServer(int serviceId, int serverId) {
        Map<Integer, List<ServerInfo>> serverInfoMap = this.serverInfoMap;
        List<ServerInfo> serverInfoList = serverInfoMap.getOrDefault(serviceId, Collections.emptyList());
        return serverInfoList.stream().anyMatch(c -> c.getServerId() == serverId);

    }


    public Set<Integer> getAllServiceId() {
        return serverInfoMap.keySet();
    }
}
