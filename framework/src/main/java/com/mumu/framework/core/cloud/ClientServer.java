/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.servlet.constants.NetConstants;
import com.mumu.framework.core.thread.ScheduledExecutorUtil;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import jakarta.annotation.Resource;
import lombok.Getter;

/**
 * ClientServer
 * 客户端连接器
 * @author liuzhen
 * @version 1.0.0 2025/3/24 22:26
 */
@Component
@ConditionalOnBean(Bootstrap.class)
public class ClientServer {
    private static final LogTopic log = LogTopic.ACTION;
    /** 尝试重连间隔（秒） */
    private static final int RETRY_INTERVAL = 5;
    /** 最大重连次数 0：无限重连 */
    private static final int MAX_RETRY_COUNT = 10;


    /** 服务器连接: ServiceType 与 serverId:channel 映射 */
    protected ConcurrentHashMap<ServiceType, Map<Integer, IoSession>> serviceIdServerIdSessionMap = new ConcurrentHashMap<>();
    /** 服务器连接心跳: channel 与 上一次心跳时间 映射 */
    protected ConcurrentHashMap<IoSession, Long> sessionKeepAliveMap = new ConcurrentHashMap<>();

    /** 本服客户端（只有注册了客户端才会获取到） */
    @Resource
    private Bootstrap nettyClient;


    /**
     * 连接服务器
     * @param remoteServers remoteServers
     * @date 2025/3/28 23:01
     */
    public void connectServers(Map<ServiceType, List<ServerInfo>> remoteServers) {
        log.info("reconnectServers", "remoteServers", remoteServers);

        long now = System.currentTimeMillis();
        try {
            for (Map.Entry<ServiceType, List<ServerInfo>> entry : remoteServers.entrySet()) {
                ServiceType serviceType = entry.getKey();
                List<ServerInfo> remoteServerInfoList = entry.getValue();
                
                // 1. 检查已连接
                Map<Integer, IoSession> serverIdSessionMap = serviceIdServerIdSessionMap.getOrDefault(serviceType, Collections.emptyMap());
                // 存在连接中的服务器
                List<Integer> willRemoveServerIdList = new ArrayList<>(4);
                for (Map.Entry<Integer, IoSession> entry1 : serverIdSessionMap.entrySet()) {
                    IoSession ioSession = entry1.getValue();
                    checkAndRemoveSessionList(serviceType, ioSession, remoteServerInfoList, willRemoveServerIdList, now);
                }


                // 2. 关闭一些服务器的连接
                removeSession(willRemoveServerIdList, serverIdSessionMap, serviceType);

                // 3. 建立新连接
                for (ServerInfo serverInfo : entry.getValue()) {
                    connect(serverInfo, RETRY_INTERVAL, MAX_RETRY_COUNT);
                }
            }
        } catch (Exception e) {
            // Utility.getTraceString(e);
            log.error(e, "reconnectServers", "remoteServers", remoteServers);
        }
    }

    private void checkAndRemoveSessionList(ServiceType serviceType, IoSession ioSession,
                                           List<ServerInfo> remoteServerInfoList, List<Integer> willRemoveServerIdList, long now) {
        ServerInfo info = ioSession.getAttr(NetConstants.SESSION_SERVER_INFO);
        int serverId = info.getServerId();
        // 1. 新拉取服务器信息未包含当前session
        if (!remoteServerInfoList.contains(info)) {
            willRemoveServerIdList.add(serverId);
        }

        // 2. 连接心跳超时删除
        long keepAlive = sessionKeepAliveMap.getOrDefault(ioSession, 0L);
        long diff = now - keepAlive;
        // TODO 60s 删除
        long overtimeLimit = 60 * 1000;
        if (diff > overtimeLimit) {
            willRemoveServerIdList.add(serverId);
            log.info("ioSession keepAlive overtime", "serviceType", serviceType, "serverId", info.getServerId(), "diff", diff);
        }

        // 存在那个session
        if (!willRemoveServerIdList.contains(serverId)) {
            remoteServerInfoList.remove(info);
        }
    }

    private void removeSession(List<Integer> willRemoveServerIdList, Map<Integer, IoSession> serverIdSessionMap, ServiceType serviceType) {
        log.info("will delete servers {}", willRemoveServerIdList);
        for (int deleteServerId : willRemoveServerIdList) {
            IoSession ioSession = serverIdSessionMap.get(deleteServerId);
            if (ioSession != null) {
                sessionKeepAliveMap.remove(ioSession);
            }
            serverIdSessionMap.remove(deleteServerId);
        }
    }


    /** 连接指定服务器 */
    private void connect(ServerInfo serverInfo) {
        connect(serverInfo, RETRY_INTERVAL, MAX_RETRY_COUNT);
    }

    /** 连接指定服务器 */
    private void connect(ServerInfo serverInfo, int retryInterval, int retryCount) {
        ConnectionRequestTask requestTask = new ConnectionRequestTask(serverInfo, retryInterval, retryCount);
        // boolean contains = connectingMap.contains(requestTask);
        // LogTopic.NET.info();

        // connectingMap.add(request);
        // 开始连接任务
        ScheduledExecutorUtil.schedule(requestTask, requestTask.getRetryInterval(), TimeUnit.SECONDS);
    }


    /**
     * @Title closeSessions
     * @Description 关闭与所有逻辑服务器的连接
     * @return void
     */
    public void closeSessions() {
        for (Map.Entry<ServiceType, Map<Integer, IoSession>> entry : serviceIdServerIdSessionMap.entrySet()) {
            for (Map.Entry<Integer, IoSession> entry1 : entry.getValue().entrySet()) {
                entry1.getValue().close();
            }
        }

        this.serviceIdServerIdSessionMap.clear();
        this.sessionKeepAliveMap.clear();
        // log.info("close all server connections completely");
    }


    public boolean isServiceUnavailable(ServiceType type) {
        if (type == ServiceType.GATE) {
            return false;
        }

        return !serviceIdServerIdSessionMap.getOrDefault(type, Collections.emptyMap()).isEmpty();
    }
    

    /**
     * 连接请求任务
     * @author liuzhen
     * @date 2025/3/28 22:42
     */
    @Getter
    public class ConnectionRequestTask implements Runnable {
        /** 服务器信息 */
        private final ServerInfo serverInfo;
        /** 服务组 */
        private final ServiceType serviceType;
        /** serverId */
        private final int serverId;
        /** ip */
        private final String ip;
        /** 端口 */
        private final int port;
        /** 尝试重连间隔（秒） */
        private final int retryInterval;
        /** 最大重连次数 0：无限重连 */
        private final int maxRetryCount;
        /** 已尝试重连次数 */
        private int retryCount;
        /** 是否成功 */
        private volatile boolean successed;

        public ConnectionRequestTask(ServerInfo serverInfo, int retryInterval, int maxRetryCount) {
            this.serverInfo = serverInfo;
            this.serviceType = serverInfo.getServiceType();
            this.serverId = serverInfo.getServerId();
            this.ip = serverInfo.getHost();
            this.port = serverInfo.getPort();
            this.retryInterval = retryInterval;
            this.maxRetryCount = maxRetryCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ConnectionRequestTask requestTask = (ConnectionRequestTask) o;
            return port == requestTask.port && serviceType == requestTask.serviceType && Objects.equals(ip, requestTask.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceType, ip, port);
        }

        /** 计算下次重连等待时间 */
        private long calRetryInterval() {
            return (long) retryCount * retryInterval + RandomUtil.randomInt(0, retryInterval);
        }

        @Override
        public void run() {
            nettyClient.connect(ip, port).addListener((ChannelFutureListener)future -> {
                if (future.isSuccess()) {
                    success(future);
                } else {
                    retry();
                }
            });
        }

        /** 连接成功 */
        private void success(ChannelFuture connect) {
            successed = true;
            // 设置基本属性
            IoSession ioSession = IoSession.of(connect.channel());
            ioSession.setAttr(NetConstants.SESSION_CLIENT, true);
            ioSession.setAttr(NetConstants.SESSION_SERVICE_TYPE, serviceType);
            ioSession.setAttr(NetConstants.SESSION_SERVER_ID, serverId);
            ioSession.setAttr(NetConstants.SESSION_SERVER_INFO, serverInfo);

            LogTopic.NET.info("ConnectServer success", "serviceType", serviceType, "connectIp", ip, "connectPort", port);

            // TODO 发送本服信息进行握手
            // MessageSender.send(IoSession.of(channel), Cmd.ReqServerInfoHandshake, serverInfo.build());
            // connectingMap.remove(this);

            // 记录channel
            Map<Integer, IoSession> serverIdChannelMap = serviceIdServerIdSessionMap.computeIfAbsent(serviceType, k -> new HashMap<>());
            serverIdChannelMap.put(serverId, ioSession);

            sessionKeepAliveMap.put(ioSession, System.currentTimeMillis());
        }

        /** 失败重试 */
        private void retry() {
            ++retryCount;
            long interval = calRetryInterval();
            LogTopic.NET.warn("ConnectServer fail retry", serviceType, "connectIp", ip, "connectPort", port, "interval",
                interval, "retryCount", retryCount, "maxRetryCount", maxRetryCount);
            if (maxRetryCount == 0 || retryCount < maxRetryCount) {
                ScheduledExecutorUtil.schedule(this, interval, TimeUnit.SECONDS);
            }
        }
    }
}
