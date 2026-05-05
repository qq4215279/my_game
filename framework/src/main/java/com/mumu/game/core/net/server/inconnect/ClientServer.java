/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.server.inconnect;

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

import com.mumu.game.core.cmd.enums.RpcCmd;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.net.helper.GameMessageFactory;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.core.net.server.IoSession;
import com.mumu.game.core.thread.ScheduledExecutorUtil;
import com.mumu.game.proto.message.server.ClientServerBean;
import com.mumu.game.proto.message.server.ReconnectServerMsgEA;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import jakarta.annotation.Resource;
import lombok.Getter;

/**
 * ClientServer
 * 客户端连接器 - 负责管理当前服务作为客户端连接到其他远程服务器的连接管理
 * 主要功能：
 * 1. 维护与远程服务器的长连接（按 ServiceType 和 serverId 分组）
 * 2. 自动重连机制（支持配置重试间隔和最大重试次数）
 * 3. 连接心跳检测（超时自动清理失效连接）
 * 4. 动态增删连接（根据远程服务器列表变化自动调整连接）
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/24 22:26
 */
@Component
@ConditionalOnBean(Bootstrap.class)
public class ClientServer {
    private static final LogTopic log = LogTopic.ACTION;

    /** 尝试重连间隔（秒） */
    private static final int RETRY_INTERVAL = 5;
    /** 最大重连次数，0表示无限重连 */
    private static final int MAX_RETRY_COUNT = 10;
    /** 连接心跳超时时间（秒），超过此时间未收到心跳则认为连接失效 */
    private static final long OVER_TIME_LIMIT = TimeUnit.SECONDS.toSeconds(60);


    /** 服务器连接映射表：ServiceType -> (serverId -> IoSession) */
    protected ConcurrentHashMap<ServiceType, Map<Integer, IoSession>> serviceIdServerIdSessionMap = new ConcurrentHashMap<>();
    /** 会话心跳时间映射表：IoSession -> 最后心跳时间戳（毫秒） */
    protected ConcurrentHashMap<IoSession, Long> sessionKeepAliveMap = new ConcurrentHashMap<>();

    /** 本服客户端 Bootstrap（只有注册了客户端才会获取到） */
    @Resource
    private Bootstrap nettyClient;


    /**
     * 连接远程服务器集群
     * 根据提供的远程服务器列表建立连接，并清理已断开或超时的旧连接
     *
     * @param remoteServers 远程服务器列表，key为服务类型，value为该类型的服务器信息列表
     * @since 2025/3/28 23:01
     */
    public void connectServers(Map<ServiceType, List<ClientServerBean>> remoteServers) {
        log.info("reconnectServers", "remoteServers", remoteServers);

        long now = System.currentTimeMillis();
        try {
            for (Map.Entry<ServiceType, List<ClientServerBean>> entry : remoteServers.entrySet()) {
                ServiceType serviceType = entry.getKey();
                List<ClientServerBean> remoteServerInfoList = entry.getValue();

                // 1. 检查已存在的连接，标记需要移除的连接
                Map<Integer, IoSession> serverIdSessionMap = serviceIdServerIdSessionMap.getOrDefault(serviceType, Collections.emptyMap());
                // 存储需要移除的 serverId 列表
                List<Integer> willRemoveServerIdList = new ArrayList<>(4);
                for (Map.Entry<Integer, IoSession> entry1 : serverIdSessionMap.entrySet()) {
                    IoSession ioSession = entry1.getValue();
                    checkAndRemoveSessionList(serviceType, ioSession, remoteServerInfoList, willRemoveServerIdList, now);
                }

                // 2. 关闭标记的旧连接
                removeSession(willRemoveServerIdList, serverIdSessionMap, serviceType);

                // 3. 建立新连接（只连接远程服务器列表中尚未连接的服务器）
                for (ClientServerBean serverInfo : entry.getValue()) {
                    connect(serverInfo, RETRY_INTERVAL, MAX_RETRY_COUNT);
                }
            }
        } catch (Exception e) {
            log.error(e, "reconnectServers", "remoteServers", remoteServers);
        }
    }

    /**
     * 检查会话是否需要移除
     * 检查条件：
     * 1. 会话对应的服务器不在新的远程服务器列表中
     * 2. 会话心跳超时（超过 OVER_TIME_LIMIT 未更新）
     *
     * @param serviceType 服务类型
     * @param ioSession 待检查的会话
     * @param remoteServerInfoList 新的远程服务器信息列表
     * @param willRemoveServerIdList 需要移除的 serverId 列表（输出参数）
     * @param now 当前时间戳（毫秒）
     */
    private void checkAndRemoveSessionList(ServiceType serviceType, IoSession ioSession,
                                           List<ClientServerBean> remoteServerInfoList, List<Integer> willRemoveServerIdList, long now) {
        ClientServerBean info = ioSession.getAttr(NetConstants.SESSION_SERVER_INFO);
        int serverId = info.getServerId();

        // 1. 新拉取的服务器信息未包含当前session，标记为需要移除
        if (!remoteServerInfoList.contains(info)) {
            willRemoveServerIdList.add(serverId);
        }

        // 2. 检查连接心跳是否超时，超时则标记为需要移除
        long keepAlive = sessionKeepAliveMap.getOrDefault(ioSession, 0L);
        long diff = now - keepAlive;
        if (diff > OVER_TIME_LIMIT) {
            willRemoveServerIdList.add(serverId);
            log.info("ioSession keepAlive overtime", "serviceType", serviceType, "serverId", info.getServerId(), "diff", diff);
        }

        // 3. 如果session仍然存在且有效，从远程服务器列表中移除（避免重复连接）
        if (!willRemoveServerIdList.contains(serverId)) {
            remoteServerInfoList.remove(info);
        }
    }

    /**
     * 关闭指定服务器的连接并清理相关数据
     *
     * @param willRemoveServerIdList 需要移除的 serverId 列表
     * @param serverIdSessionMap 当前服务类型下的 serverId -> IoSession 映射
     * @param serviceType 服务类型
     */
    private void removeSession(List<Integer> willRemoveServerIdList, Map<Integer, IoSession> serverIdSessionMap, ServiceType serviceType) {
        log.info("will delete servers {}", willRemoveServerIdList);
        for (int deleteServerId : willRemoveServerIdList) {
            IoSession ioSession = serverIdSessionMap.get(deleteServerId);
            if (ioSession != null) {
                // 移除心跳记录
                sessionKeepAliveMap.remove(ioSession);
                // 关闭连接
                ioSession.close();
            }
            // 移除会话映射
            serverIdSessionMap.remove(deleteServerId);
        }
    }


    /**
     * 连接指定服务器（使用默认重试策略）
     *
     * @param clientServerInfo 服务器信息
     */
    private void connect(ClientServerBean clientServerInfo) {
        connect(clientServerInfo, RETRY_INTERVAL, MAX_RETRY_COUNT);
    }

    /**
     * 连接指定服务器（自定义重试策略）
     *
     * @param clientServerInfo 服务器信息
     * @param retryInterval 重试间隔（秒）
     * @param retryCount 最大重试次数，0表示无限重试
     */
    private void connect(ClientServerBean clientServerInfo, int retryInterval, int retryCount) {
        ConnectionRequestTask requestTask = new ConnectionRequestTask(clientServerInfo, retryInterval, retryCount);
        // 开始连接任务（异步执行）
        ScheduledExecutorUtil.schedule(requestTask, requestTask.getRetryInterval(), TimeUnit.SECONDS);
    }


    /**
     * 关闭与所有远程服务器的连接
     * 清空所有连接映射和心跳记录
     */
    public void closeSessions() {
        for (Map.Entry<ServiceType, Map<Integer, IoSession>> entry : serviceIdServerIdSessionMap.entrySet()) {
            for (Map.Entry<Integer, IoSession> entry1 : entry.getValue().entrySet()) {
                entry1.getValue().close();
            }
        }

        this.serviceIdServerIdSessionMap.clear();
        this.sessionKeepAliveMap.clear();
    }


    /**
     * 检查指定类型的服务是否不可用
     * @param type 服务类型
     * @return true-服务不可用（无可用连接），false-服务可用
     */
    public boolean isServiceUnavailable(ServiceType type) {
        // GATEWAY 服务始终认为可用
        if (type == ServiceType.GATEWAY) {
            return false;
        }

        // 检查该服务类型下是否有活跃连接
        return !serviceIdServerIdSessionMap.getOrDefault(type, Collections.emptyMap()).isEmpty();
    }


    /**
     * ConnectionRequestTask
     * 连接请求任务 - 封装单次连接请求的逻辑，支持失败自动重试
     *
     * @author liuzhen
     * @since 2025/3/28 22:42
     */
    @Getter
    public class ConnectionRequestTask implements Runnable {
        /** 服务器信息 */
        private final ClientServerBean clientServerInfo;
        /** 服务类型 */
        private final ServiceType serviceType;
        /** 服务器ID */
        private final int serverId;
        /** 服务器IP地址 */
        private final String ip;
        /** 服务器端口 */
        private final int port;
        /** 尝试重连间隔（秒） */
        private final int retryInterval;
        /** 最大重连次数，0表示无限重连 */
        private final int maxRetryCount;
        /** 已尝试重连次数 */
        private int retryCount;
        /** 是否连接成功 */
        private volatile boolean successed;

        /**
         * 构造连接请求任务
         *
         * @param clientServerInfo 服务器信息
         * @param retryInterval 重试间隔（秒）
         * @param maxRetryCount 最大重试次数，0表示无限重试
         */
        public ConnectionRequestTask(ClientServerBean clientServerInfo, int retryInterval, int maxRetryCount) {
            this.clientServerInfo = clientServerInfo;
            this.serviceType = ServiceType.getServiceType(clientServerInfo.getServiceId());
            this.serverId = clientServerInfo.getServerId();
            this.ip = clientServerInfo.getIp();
            this.port = clientServerInfo.getPort();
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

        /**
         * 计算下次重连等待时间（递增延迟 + 随机抖动）
         * 公式：retryCount * retryInterval + random(0, retryInterval)
         *
         * @return 下次重连间隔（秒）
         */
        private long calRetryInterval() {
            return (long) retryCount * retryInterval + RandomUtil.randomInt(0, retryInterval);
        }

        @Override
        public void run() {
            // 发起异步连接
            nettyClient.connect(ip, port).addListener((ChannelFutureListener)future -> {
                if (future.isSuccess()) {
                    // 连接成功
                    success(future);
                } else {
                    // 连接失败，触发重试
                    retry();
                }
            });
        }

        /**
         * 连接成功处理
         * 1. 设置会话属性（客户端标识、服务类型、服务器ID等）
         * 2. 发送握手消息进行身份验证
         * 3. 记录连接到映射表中
         * 4. 初始化心跳时间
         *
         * @param connect 连接成功的 ChannelFuture
         */
        private void success(ChannelFuture connect) {
            successed = true;

            // 创建会话并设置基本属性
            IoSession session = IoSession.of(connect.channel());
            session.setAttr(NetConstants.SESSION_CLIENT, true);
            session.setAttr(NetConstants.SESSION_SERVICE_TYPE, serviceType);
            session.setAttr(NetConstants.SESSION_SERVER_ID, serverId);
            session.setAttr(NetConstants.SESSION_SERVER_INFO, clientServerInfo);

            LogTopic.NET.info("ConnectServer success", "serviceType", serviceType, "connectIp", ip, "connectPort", port);

            // 发送本服信息进行握手认证
            ReconnectServerMsgEA reqMsg = new ReconnectServerMsgEA();
            reqMsg.setClientServerBean(clientServerInfo);
            GameMessagePackage gameMessagePackage =
                    GameMessageFactory.reqProxy(RpcCmd.ServerInfoHandshake, 0L, null, reqMsg);
            MessageSender.send(session, gameMessagePackage);

            // 记录连接到映射表：serviceType -> (serverId -> session)
            Map<Integer, IoSession> serverIdChannelMap = serviceIdServerIdSessionMap.computeIfAbsent(serviceType, k -> new HashMap<>());
            serverIdChannelMap.put(serverId, session);

            // 初始化心跳时间
            sessionKeepAliveMap.put(session, System.currentTimeMillis());
        }

        /**
         * 连接失败重试处理
         * 1. 增加重试计数
         * 2. 计算下次重试间隔（递增延迟）
         * 3. 如果未达到最大重试次数，则调度下次重试
         */
        private void retry() {
            ++retryCount;
            long interval = calRetryInterval();
            LogTopic.NET.warn("ConnectServer fail retry", serviceType, "connectIp", ip, "connectPort", port, "interval",
                    interval, "retryCount", retryCount, "maxRetryCount", maxRetryCount);

            // 如果未达到最大重试次数（maxRetryCount=0表示无限重试），则继续重试
            if (maxRetryCount == 0 || retryCount < maxRetryCount) {
                ScheduledExecutorUtil.schedule(this, interval, TimeUnit.SECONDS);
            }
        }
    }
}
