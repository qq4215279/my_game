/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register;

import java.util.concurrent.TimeUnit;

import com.mumu.game.core.register.bo.RegisteredServerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.thread.ScheduledExecutorUtil;

import jakarta.annotation.PreDestroy;

/**
 * ServiceRegistryRegistrar
 * 服务注册器
 * 业务进程向 Redis 注册本机服务信息：首次注册写 Hash 并建立 TTL 心跳，定时任务仅续期心跳；容器销毁或主动调用时从注册中心移除实例
 * <p>
 * <b>如何触发 {@link #registerAndStartHeartbeat(RegisteredServerInfo)}：</b>
 * <ul>
 * <li>推荐（开箱）：配置 {@code net.server.enable=true}（与 Netty 服务端一致）时加载 {@link ServiceRegistryRegistrationBootstrap}，
 * 在 {@link com.mumu.game.core.net.config.NettyServerConfiguration} / {@link com.mumu.game.core.net.server.AutoServerStarter} 完成后再根据
 * {@link com.mumu.game.core.properties.ServerInfo} 自动调用。</li>
 * <li>自定义时机：在任意 Spring Bean 中注入本类，在 {@code ApplicationRunner}、{@code SmartLifecycle#start()}、或业务就绪事件里自行组装
 * {@link RegisteredServerInfo} 后调用本方法（例如仅游戏服注册、或注册前需改写 host/port）。</li>
 * </ul>
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
public class ServiceRegistryRegistrar {

    private final ServiceRegistryRepository repository;

    /**
     * 心跳 key 的 TTL（秒），应大于 {@link #heartbeatIntervalSeconds}，避免误判失联
     */
    @Value("${service.registry.heartbeat-ttl-seconds:30}")
    private int heartbeatTtlSeconds;

    /**
     * 本地定时续心跳的间隔（秒），仅调用 {@link ServiceRegistryRepository#keepHeartbeat(int, int, int)}，不触发目录版本变更
     */
    @Value("${service.registry.heartbeat-interval-seconds:10}")
    private int heartbeatIntervalSeconds;

    /**
     * 当前已注册到 Redis 的实例信息；未注册时为 null
     */
    private volatile RegisteredServerInfo current;

    /**
     * {@link com.mumu.game.core.thread.ScheduledExecutorUtil} 中调度心跳任务的 key，用于取消任务
     */
    private volatile String heartbeatTaskKey;

    /**
     * @param repository 注册中心 Redis 仓储
     */
    public ServiceRegistryRegistrar(ServiceRegistryRepository repository) {
        this.repository = repository;
    }

    /**
     * 写入注册信息并启动定时续期心跳：首次注册会递增全局版本；后续定时任务仅续 TTL
     *
     * @param record 本机实例信息，不可为空
     * @return long 注册成功后 Redis 全局版本号
     */
    public long registerAndStartHeartbeat(RegisteredServerInfo record) {
        stopHeartbeatOnly();
        this.current = record;
        // registerServer 内已完成写 Hash、SADD 类型、首次心跳与版本递增
        long ver = repository.registerServer(record, heartbeatTtlSeconds);
        int serverId = record.getServerId();
        this.heartbeatTaskKey = "service-registry-hb-" + record.getServiceTypeId() + "-" + serverId;
        // 周期续期仅刷新心跳 TTL，不触发版本变更
        ScheduledExecutorUtil.scheduleAtFixedRate(heartbeatTaskKey,
            () -> repository.keepHeartbeat(record.getServiceTypeId(), serverId, heartbeatTtlSeconds),
            heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
        LogTopic.NET.info("ServiceRegistryRegistrar.registered", "serverId", serverId, "type",
            record.getServiceTypeId(), "version", ver);
        return ver;
    }

    /**
     * 停止本地心跳调度任务，不删除 Redis 中的实例行与心跳 key（适合短暂暂停续期，慎用易导致 TTL 过期被 Gateway 清理）
     */
    public void stopHeartbeatOnly() {
        if (heartbeatTaskKey != null) {
            ScheduledExecutorUtil.cancel(heartbeatTaskKey);
            heartbeatTaskKey = null;
        }
    }


    /** 容器关闭事件 */
    @Order(10)
    @EventListener(ContextClosedEvent.class)
    public void onClosed(ContextClosedEvent event) {
        LogTopic.ACTION.info("容器关闭 ServiceRegistryRegistrar...");
        onShutdown();
    }

    /**
     * Spring 容器销毁时主动下线当前实例，缩短「进程已退出但心跳仍存活」的窗口期
     */
    private void onShutdown() {
        try {
            // 进程正常退出时主动 unregister，缩短「实例已停但心跳 TTL 未过期」的窗口期
            unregisterCurrent();
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "ServiceRegistryRegistrar.onShutdown");
        }
    }

    /**
     * 主动从注册中心移除当前实例并停止心跳调度；若当前未注册则仅返回当前全局版本
     *
     * @return long 注销后的全局版本号（未注册时返回当前 Redis 全局版本）
     */
    public long unregisterCurrent() {
        stopHeartbeatOnly();
        RegisteredServerInfo rec = this.current;
        this.current = null;
        if (rec == null) {
            return repository.getGlobalVersion();
        }
        long ver = repository.unregisterServer(rec.getServiceTypeId(), rec.getServerId());
        LogTopic.NET.info("ServiceRegistryRegistrar.unregistered", "serverId", rec.getServerId());
        return ver;
    }
}
