/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register.listener;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.redis.chanel.RedisChannelListener;
import com.mumu.game.core.redis.constants.RedisChannel;
import com.mumu.game.core.register.ServiceRegistryListener;
import com.mumu.game.core.register.ServiceRegistryRepository;
import com.mumu.game.core.register.bo.RegisteredServerInfo;
import com.mumu.game.core.register.bo.ServiceRegistrySnapshot;
import com.mumu.game.core.thread.ScheduledExecutorUtil;

/**
 * ServiceRegistryDiscoveryListener
 * 服务注册发现客户端监听：订阅 SERVICE_REGISTRY 频道触发延迟拉取，并定时轮询全局版本号作为 Pub/Sub 丢消息的补偿；版本落后时全量拉取快照并回调监听方
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
@ConditionalOnProperty(prefix = "net.client", name = "enable", havingValue = "true")
public class ServiceRegistryDiscoveryListener implements AutoInitEvent,
        RedisChannelListener<ServiceRegistryChangeMessage> {

    /**
     * 定时比对 Redis 全局版本与本地版本的任务在调度器中的唯一 key
     */
    private static final String TASK_VERSION_POLL = "service-registry-version-poll";
    /**
     * Pub/Sub 触发后的防抖延迟拉取任务在调度器中的唯一 key
     */
    private static final String TASK_DEBOUNCE = "service-registry-debounce-sync";

    private final ServiceRegistryRepository repository;

    /**
     * 目录更新回调列表；无 Bean 时由 Spring 注入空列表
     */
    private final List<ServiceRegistryListener> listeners;

    /**
     * 本地已处理的全局版本号，仅当 Redis 版本更大时才拉取快照
     */
    private final AtomicLong localVersion = new AtomicLong(0L);

    /**
     * 定时轮询全局版本的间隔（秒），作为 Pub/Sub 不可靠时的补偿
     */
    @Value("${service.registry.version-poll-interval-seconds:10}")
    private long versionPollIntervalSeconds;

    /**
     * 收到订阅消息后延迟执行的毫秒数，用于合并短时间内的多次通知
     */
    @Value("${service.registry.pubsub-debounce-millis:150}")
    private long pubsubDebounceMillis;

    /**
     * @param repository 注册中心 Redis 仓储
     * @param listeners  目录变更监听方列表，允许为空
     */
    public ServiceRegistryDiscoveryListener(ServiceRegistryRepository repository,
                                            List<ServiceRegistryListener> listeners) {
        this.repository = repository;
        this.listeners = listeners == null ? List.of() : listeners;
    }

    @Override
    public void autoInit() {
        start();
    }

    /**
     * 启动时立即尝试同步一次目录，并注册定时版本轮询任务
     */
    private void start() {
        // 启动时先对齐一次，避免「已错过订阅消息」期间目录不为空却本地版本为 0
        syncFullIfNeeded();
        // 定时比对 Redis 全局版本与本地版本，作为 Pub/Sub 不可靠场景的兜底
        ScheduledExecutorUtil.scheduleWithFixedDelay(TASK_VERSION_POLL, this::syncFullIfNeeded,
            versionPollIntervalSeconds, versionPollIntervalSeconds, TimeUnit.SECONDS);
        LogTopic.NET.info("ServiceRegistryDiscoveryListener started", "versionPollSec", versionPollIntervalSeconds);
    }


    /**
     * 容器关闭事件
     * 取消已注册的定时任务，避免容器关闭后仍访问 Redis
     * */
    @Order(10)
    @EventListener(ContextClosedEvent.class)
    public void onClosed(ContextClosedEvent event) {
        ScheduledExecutorUtil.cancel(TASK_VERSION_POLL);
        ScheduledExecutorUtil.cancel(TASK_DEBOUNCE);
    }

    /**
     * 声明订阅的 Redis 频道，与 {@link com.mumu.game.core.redis.config.RedisConfiguration} 中监听器绑定一致
     * @return com.mumu.game.core.redis.constants.RedisChannel 注册中心变更频道 {@link RedisChannel#SERVICE_REGISTRY}
     */
    @Override
    public RedisChannel subscribeChannel() {
        return RedisChannel.SERVICE_REGISTRY;
    }

    /**
     * Redis 推送目录变更时回调：不直接拉取，仅调度防抖任务，避免在订阅线程执行重 IO
     *
     * @param channel Redis 频道名
     * @param message 变更消息体，含新版本号与涉及类型；可为 null（忽略）
     */
    @Override
    public void onMessage(String channel, ServiceRegistryChangeMessage message) {
        if (message == null) {
            return;
        }
        // 延迟拉取标志：合并短时间内的多次通知，避免在订阅线程上做重 IO
        ScheduledExecutorUtil.schedule(TASK_DEBOUNCE, this::syncFullIfNeeded, pubsubDebounceMillis,
            TimeUnit.MILLISECONDS, true);
    }

    /**
     * 若 Redis 全局版本高于本地已处理版本，则拉取全量快照、更新本地版本并依次回调
     */
    public void syncFullIfNeeded() {
        try {
            long remote = repository.getGlobalVersion();
            // 远程版本不大于本地已处理版本，说明目录无新变更，跳过重 IO
            if (remote <= localVersion.get()) {
                return;
            }
            ServiceRegistrySnapshot snapshot = repository.loadSnapshot();
            localVersion.set(snapshot.getVersion());
            if (listeners.isEmpty()) {
                return;
            }

            // 拷贝一份再回调，降低监听方误改内部 Map 影响 discovery 客户端缓存的风险
            ServiceRegistrySnapshot readOnlyCopy = ServiceRegistrySnapshot.builder().version(snapshot.getVersion())
                .serviceServerIdInfoMap(copyByType(snapshot.getServiceServerIdInfoMap())).build();
            for (ServiceRegistryListener listener : listeners) {
                try {
                    listener.onSnapshotUpdated(readOnlyCopy);
                } catch (Exception e) {
                    LogTopic.ACTION.error(e, "ServiceRegistryListener.onSnapshotUpdated");
                }
            }
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "ServiceRegistryDiscoveryListener.syncFullIfNeeded");
        }
    }

    /**
     * 对按类型分组的实例 Map 做浅拷贝，降低监听方误改内部结构的风险
     *
     * @param src 快照中的 byType 数据，允许为 null
     * @return java.util.Map&lt;java.lang.Integer, java.util.Map&lt;java.lang.String, com.mumu.game.core.register.bo.RegisteredServerInfo&gt;&gt;
     *         外层 key 为 serviceTypeId，内层 key 为 serverId
     */
    private static Map<Integer, Map<String, RegisteredServerInfo>> copyByType(
        Map<Integer, Map<String, RegisteredServerInfo>> src) {
        Map<Integer, Map<String, RegisteredServerInfo>> out = new LinkedHashMap<>();
        if (src == null) {
            return out;
        }
        for (var e : src.entrySet()) {
            out.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        }
        return out;
    }

    /**
     * 当前进程已处理的全局目录版本号（最近一次成功拉取快照后的值）
     *
     * @return long 本地版本，小于等于 Redis 当前版本
     */
    public long getLocalVersion() {
        return localVersion.get();
    }
}
