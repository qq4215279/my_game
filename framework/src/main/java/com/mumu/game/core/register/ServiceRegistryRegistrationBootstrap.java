/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register;

import com.mumu.game.core.register.bo.RegisteredServerInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.config.NettyServerConfiguration;
import com.mumu.game.core.net.server.AutoServerStarter;
import com.mumu.game.core.properties.ServerInfo;

/**
 * ServiceRegistryRegistrationBootstrap
 * 可选的自动注册：在 {@link NettyServerConfiguration} 完成 Bean 装配且 {@link AutoServerStarter} 已绑定监听端口之后，
 * 根据 {@link ServerInfo} 调用 {@link ServiceRegistryRegistrar#registerAndStartHeartbeat(RegisteredServerInfo)}。
 * 需 {@code net.server.enable=true}（与 Netty 服务端开关一致）。
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
@ConditionalOnProperty(prefix = "net.server", name = "enable", havingValue = "true")
// 确保名为 "nettyServer" 的Bean在当前Bean 初始化之前启动
@DependsOn("nettyServer")
public class ServiceRegistryRegistrationBootstrap implements SmartLifecycle {

    private final ServerInfo serverInfo;
    private final ServiceRegistryRegistrar registrar;

    private volatile boolean running = false;

    public ServiceRegistryRegistrationBootstrap(ServerInfo serverInfo, ServiceRegistryRegistrar registrar) {
        this.serverInfo = serverInfo;
        this.registrar = registrar;
    }

    @Override
    public void start() {
        // 使用与 Netty/配置一致的对外 host、port、serverId、serviceType
        RegisteredServerInfo record = RegisteredServerInfo.of(serverInfo);
        registrar.registerAndStartHeartbeat(record);
        running = true;
        LogTopic.NET.info("ServiceRegistryRegistrationBootstrap 已自动注册", "serverId", record.getServerId(), "type",
                record.getServiceTypeId(), "host", record.getHost(), "port", record.getPort());
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 紧接在 {@link AutoServerStarter} 之后启动：同一套 Netty 服务端场景下，先 phase=0 绑定端口，再执行本类注册 Redis
     */
    @Override
    public int getPhase() {
        return AutoServerStarter.LIFECYCLE_PHASE + 1;
    }
}
