/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.net.consts.ServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.thread.ScheduledExecutorUtil;

import jakarta.annotation.PostConstruct;

/**
 * ServiceRegistryGatewayAligner
 * 网关侧定时任务：将 Redis 中「实例 Hash」与「TTL 心跳 key」对齐，剔除心跳已过期但仍残留在 Hash 中的节点，并触发版本递增与 Pub/Sub 通知。
 * 仅当配置项 {@code net.serviceType=GATEWAY} 时注册本 Bean（与 {@link org.springframework.boot.autoconfigure.condition.ConditionalOnProperty} 一致）
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
@ConditionalOnProperty(name = "net.serviceType", havingValue = "GATEWAY")
public class ServiceRegistryGatewayAligner implements AutoInitEvent {

    private final ServiceRegistryRepository repository;

    /**
     * 首次执行对齐任务前的延迟（秒）
     */
    @Value("${game.common.registry.gateway-align-initial-delay-seconds:5}")
    private long alignInitialDelaySeconds;

    /**
     * 两次对齐任务之间的固定间隔（秒）
     */
    @Value("${game.common.registry.gateway-align-interval-seconds:30}")
    private long alignIntervalSeconds;

    /**
     * @param repository 注册中心 Redis 仓储，用于执行僵尸实例清理
     */
    public ServiceRegistryGatewayAligner(ServiceRegistryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void autoInit() {
        startAlignSchedule();
    }

    /**
     * 容器启动后调度定时对齐任务（本 Bean 仅当配置 {@code net.serviceType=GATEWAY} 时由 Spring 创建）
     */
    private void startAlignSchedule() {
        ScheduledExecutorUtil.scheduleWithFixedDelay("service-registry-gateway-purge", this::safePurge,
            alignInitialDelaySeconds, alignIntervalSeconds, TimeUnit.SECONDS);
        LogTopic.NET.info("ServiceRegistryGatewayAligner started", "initialDelaySec", alignInitialDelaySeconds,
            "intervalSec", alignIntervalSeconds);
    }

    /**
     * 包装一层异常捕获，避免定时任务线程因单次 Redis 异常终止后续调度
     */
    private void safePurge() {
        try {
            // 委托仓储层扫描并删除无心跳实例；内部仅在确有删除时 INCR 版本并发布
            ServiceRegistryRepository.PurgeResult r = repository.purgeInstancesWithoutHeartbeat();
            if (r.isChanged()) {
                LogTopic.NET.info("serviceRegistry.purged", "newVersion", r.getNewVersion(), "types",
                    r.getAffectedTypeIds());
            }
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "service-registry-gateway-purge");
        }
    }

    @Override
    public Collection<ServiceType> loadService() {
        return List.of(ServiceType.GATEWAY);
    }
}
