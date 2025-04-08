/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import org.jctools.maps.NonBlockingHashMap;
import org.springframework.stereotype.Component;

import com.mumu.common.collection.ConcurrentMultiKeyMap;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.cloud.ServerInfo;
import com.mumu.framework.core.mvc.constants.NetConstants;
import com.mumu.framework.core.mvc.constants.ServiceType;
import com.mumu.framework.core.mvc.server.IoSession;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.channel.Channel;

/**
 * PlayerSessionManager
 * 玩家session 管理类
 * @author liuzhen
 * @version 1.0.0 2025/3/29 9:54
 */
@Component
public class SessionManager {
    private static final LogTopic log = LogTopic.ACTION;
    
    public static SessionManager self() {
        return SpringContextUtils.getBean(SessionManager.class);
    }


    /** playerId 与 IoSession的映射容器，这里使用的是HashMap，所以，对于Map的操作都要放在锁里面 */
    private final Map<Long, IoSession> playerSessionMap = new HashMap<>();
    /** 读写锁,使用非公平锁 */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    /** 缓存的全部连接 key: sessionId */
    private final NonBlockingHashMap<String, IoSession> allSessionMap = new NonBlockingHashMap<>();

    /** 连接的其他服务信息 key1: ServiceType; key2: serverId; value:  */
    private final ConcurrentMultiKeyMap<ServiceType, Integer, IoSession> serverSessionMap =
            ConcurrentMultiKeyMap.create();



    public IoSession getPlayerSession(long playerId) {
        lock.readLock().lock();
        try {
            return this.playerSessionMap.get(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addPlayerSession(long playerId, IoSession ioSession) {
        // 数据写入，添加写锁
        this.writeLock(() -> {
            ioSession.setAttr(NetConstants.SESSION_PLAYER_ID, playerId);
            playerSessionMap.put(playerId, ioSession);
        });
    }

    public void removePlayerSession(long playerId, Channel removedSession) {
        this.writeLock(() -> {
            IoSession existIoSession = playerSessionMap.get(playerId);
            // 必须是同一个对象才可以移除
            if (existIoSession != null && existIoSession.channel() == removedSession) {
                playerSessionMap.remove(playerId);
                existIoSession.close();
            }
        });
    }


    /**
     * 向Channel广播消息
     * @param consumer consumer
     * @return void
     * @date 2024/6/19 17:42
     */
    public void broadcast(BiConsumer<Long, IoSession> consumer) {
        this.readLock(() -> {
            this.playerSessionMap.forEach(consumer);
        });
    }

    /**
     * 封装添加写锁，统一添加，防止写错
     * @param task task
     * @return void
     * @date 2024/6/19 17:41
     */
    private void writeLock(Runnable task) {
        lock.writeLock().lock();
        try {
            task.run();

            // 统一异常捕获
        } catch (Exception e) {
            log.error("ChannelService写锁处理异常", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 封装添加读锁，统一添加，防止写错
     * @param task task
     * @return void
     * @date 2024/6/19 17:41
     */
    private void readLock(Runnable task) {
        lock.readLock().lock();
        try {
            task.run();

            // 统一异常捕获
        } catch (Exception e) {
            log.error("ChannelService读锁处理异常", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getChannelCount() {
        lock.writeLock().lock();
        try {
            // 获取连锁的数量
            return this.playerSessionMap.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 根据服务id，获取连接 */
    public IoSession getServerSession(ServiceType serviceType, int serverId) {
        return serverSessionMap.get(serviceType, serverId);
    }

    public boolean containsServerId(ServiceType serviceType, int serverId) {
        return serverSessionMap.contains(serviceType, serverId);
    }

    // TODO 进行握手
    public void addServerSession(IoSession ioSession, ServerInfo serverInfo) {
        // 标记
        ioSession.setAttr(NetConstants.SESSION_SERVICE_TYPE, serverInfo.getServiceType());
        ioSession.setAttr(NetConstants.SESSION_SERVER_ID, serverInfo.getServerId());
        ioSession.setAttr(NetConstants.SESSION_SERVER_INFO, serverInfo);

        // 缓存服务id-session
        IoSession oldSession = serverSessionMap.get(serverInfo.getServiceType(), serverInfo.getServerId());
        serverSessionMap.put(serverInfo.getServiceType(), serverInfo.getServerId(), ioSession);
        if (oldSession != null) {
            LogTopic.NET.warn("registerServer session replaced", "serverId", serverInfo.getServerId(), "oldSession", oldSession, "session", ioSession);
        }
    }

    /** 移除 session */
    public IoSession removeServerSession(String sessionKey) {
        IoSession session = allSessionMap.remove(sessionKey);
        if (session == null) {
            return null;
        }
        
        // 移除服务器session和serverInfo缓存
        Integer serverId = session.getAttr(NetConstants.SESSION_SERVER_ID);
        ServiceType serviceType = session.getAttr(NetConstants.SESSION_SERVICE_TYPE);
        if (serverId != null && serviceType != null) {
            serverSessionMap.remove(serviceType, serverId);
        }

        return session;
    }

    /** 添加 session */
    public void add(IoSession session) {
        allSessionMap.put(session.getKey(), session);
    }

    /** 获取 session */
    public IoSession get(String sessionKey) {
        return allSessionMap.get(sessionKey);
    }

    /** 判断 session存在 */
    public boolean contains(String sessionKey) {
        return allSessionMap.containsKey(sessionKey);
    }

}
