/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Component;

import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.servlet.constants.NetConstants;
import com.mumu.framework.util.SpringContextUtils;

import io.netty.channel.Channel;

/**
 * PlayerSessionManager
 * 玩家session 管理类
 * @author liuzhen
 * @version 1.0.0 2025/3/29 9:54
 */
@Component
public class PlayerSessionManager {
    private static final LogTopic log = LogTopic.ACTION;
    
    public static PlayerSessionManager self() {
        return SpringContextUtils.getBean(PlayerSessionManager.class);
    }

    /**
     * gateServerId:IoSession
     */
    private Map<Integer, IoSession> gateSessionMap = new HashMap<>();

    /**
     * playerId与Netty。Channel的映射容器，这里使用的是HashMap，所以，对于Map的操作都要放在锁里面
     */
    private Map<Long, IoSession> playerChannelMap = new HashMap<>();
    /** 读写锁,使用非公平锁 */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public IoSession getGateChannel(int gateServerId) {
        lock.readLock().lock();
        try {
            return this.gateSessionMap.get(gateServerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    // TODO 添加握手协议
    public void addGateSession(int gateServerId, IoSession ioSession) {
        // 数据写入，添加写锁
        this.writeLock(() -> {
            gateSessionMap.put(gateServerId, ioSession);
        });
    }

    // TODO
    public void removeSession(IoSession removedSession) {
        Integer gateServerId = removedSession.getAttr(NetConstants.SESSION_SERVER_ID);
        if (gateServerId != null) {
            if (gateSessionMap.containsKey(gateServerId)) {
                this.writeLock(() -> {
                    IoSession existIoSession = gateSessionMap.get(gateServerId);
                    // 必须是同一个对象才可以移除
                    if (existIoSession != null && existIoSession.channel() == removedSession.channel()) {
                        gateSessionMap.remove(gateServerId);
                        existIoSession.close();
                    }
                });

            }
        }

    }

    public IoSession getChannel(long playerId) {
        lock.readLock().lock();
        try {
            return this.playerChannelMap.get(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addChannel(long playerId, IoSession ioSession) {
        // 数据写入，添加写锁
        this.writeLock(() -> {
            playerChannelMap.put(playerId, ioSession);
        });
    }

    public void removeChannel(long playerId, Channel removedSession) {
        this.writeLock(() -> {
            IoSession existIoSession = playerChannelMap.get(playerId);
            // 必须是同一个对象才可以移除
            if (existIoSession != null && existIoSession.channel() == removedSession) {
                playerChannelMap.remove(playerId);
                existIoSession.close();
            }
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
     * 向Channel广播消息
     * @param consumer consumer
     * @return void
     * @date 2024/6/19 17:42
     */
    public void broadcast(BiConsumer<Long, IoSession> consumer) {
        this.readLock(() -> {
            this.playerChannelMap.forEach(consumer);
        });
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
            int size = this.playerChannelMap.size();
            return size;
        } finally {
            lock.writeLock().unlock();
        }
    }


}
