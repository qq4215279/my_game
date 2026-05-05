/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import com.mumu.game.proto.message.server.ClientServerBean;
import com.mumu.game.proto.server.ClientServerInfo;
import org.jctools.maps.NonBlockingHashMap;
import org.springframework.stereotype.Component;

import com.mumu.game.collection.ConcurrentMultiKeyMap;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.IoSession;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.utils.SpringContextUtils;

import io.netty.channel.Channel;
import jakarta.annotation.Resource;

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

    /** 玩家信息管理 */
    @Resource
    PlayerManager playerManager;

    /** 读写锁,使用非公平锁 */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** playerId 与 IoSession的映射容器，这里使用的是HashMap，所以，对于Map的操作都要放在锁里面 */
    private final Map<Long, IoSession> playerSessionMap = new HashMap<>();
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

    /**
     * 添加玩家session
     * @param playerId 玩家id
     * @param ioSession session
     */
    public void addPlayerSession(long playerId, IoSession ioSession) {
        // 数据写入，添加写锁
        this.writeLock(() -> {
            ioSession.setAttr(NetConstants.SESSION_PLAYER_ID, playerId);
            playerSessionMap.put(playerId, ioSession);
        });
    }

    /**
     * 移除玩家session
     * @param playerId 玩家id
     * @param removedSession 移除的session
     */
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
     * @since 2024/6/19 17:42
     */
    public void broadcast(BiConsumer<Long, IoSession> consumer) {
        this.readLock(() -> {
            this.playerSessionMap.forEach(consumer);
        });
    }

    /**
     * 封装添加写锁，统一添加，防止写错
     * @param task task
     * @since 2024/6/19 17:41
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
     * @since 2024/6/19 17:41
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


    // serverSessionMap =========>
    /** 根据服务id，获取连接 */
    public IoSession getServerSession(ServiceType serviceType, int serverId) {
        return serverSessionMap.get(serviceType, serverId);
    }

    public boolean containsServerId(ServiceType serviceType, int serverId) {
        return serverSessionMap.contains(serviceType, serverId);
    }

    /**
     * 添加服务器session
     * @param session session
     * @param clientServerInfo 服务器信息
     */
    public void addServerSession(IoSession session, ClientServerBean clientServerInfo) {
        ServiceType serviceType = ServiceType.getServiceType(clientServerInfo.getServiceId());

        // 标记
        session.setAttr(NetConstants.SESSION_SERVICE_TYPE, serviceType);
        session.setAttr(NetConstants.SESSION_SERVER_ID, clientServerInfo.getServerId());
        session.setAttr(NetConstants.SESSION_SERVER_INFO, clientServerInfo);

        // 缓存服务id-session
        IoSession oldSession = serverSessionMap.get(serviceType, clientServerInfo.getServerId());
        serverSessionMap.put(serviceType, clientServerInfo.getServerId(), session);
        if (oldSession != null) {
            LogTopic.NET.warn("registerServer session replaced", "serverId", clientServerInfo.getServerId(), "oldSession", oldSession, "session", session);
        }
    }

    /** 移除 session */
    public IoSession removeSession(String sessionKey) {
        IoSession session = allSessionMap.remove(sessionKey);
        if (session == null) {
            return null;
        }
        
        // 移除服务器session和serverInfo缓存
        ServiceType serviceType = session.getAttr(NetConstants.SESSION_SERVICE_TYPE);
        Integer serverId = session.getAttr(NetConstants.SESSION_SERVER_ID);
        if (serverId != null && serviceType != null) {
            serverSessionMap.remove(serviceType, serverId);
        }

        // 移除服务器玩家session缓存
        Long playerId = session.getAttr(NetConstants.SESSION_PLAYER_ID);
        // 需要判断下关闭的session是否为当前session，避免重登陆时关闭老session后，将新session移除
        if (playerId != null && playerSessionMap.get(playerId) == session) {
            playerSessionMap.remove(playerId);
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



    // ==================> 获取session
    /** 玩家数据下行session,即给玩家发消息(Gate服会返回玩家连接,其他服返回玩家所在Gate的连接) */
    public IoSession getOutSession(long playerId) {
        IoSession session = playerSessionMap.get(playerId);
        if (session != null) {
            return session;
        }

        // 获取玩家所在网关
        return getByServerId(ServiceType.GATEWAY, playerManager.getServerId(playerId, ServiceType.GATEWAY));
    }

    /** 玩家数据上行session,即接收玩家消息,路由到玩家所在服务器 */
    public IoSession getInSession(long playerId, ServiceType serviceType) {
        return getByServerId(serviceType, getInServerId(playerId, serviceType));
    }

    /** 根据服务id，获取连接 */
    public IoSession getByServerId(ServiceType serviceType, int serverId) {
        return serverSessionMap.get(serviceType, serverId);
    }

    /** 获取玩家所在服（不存在则会找一个） */
    public int getInServerId(long playerId, ServiceType serviceType) {
        // 获取玩家所在服务器的连接，目标服务连接不存在时，查找一个
        int serverId = playerManager.getServerId(playerId, serviceType);
        if (containsServerId(serviceType, serverId)) {
            return serverId;
        }

        return serverId;
    }

}
