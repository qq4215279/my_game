/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mumu.game.core.register.bo.RegisteredServerInfo;
import com.mumu.game.core.register.bo.ServiceRegistrySnapshot;
import com.mumu.game.core.register.listener.ServiceRegistryChangeMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.redis.RedisUtil;
import com.mumu.game.core.redis.constants.RedisChannel;
import com.mumu.game.core.redis.constants.RedisKey;
import com.mumu.game.utils.JsonUtil;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

/**
 * ServiceRegistryRepository
 * 服务注册中心 Redis 访问层：维护全局版本号、按类型的实例 Hash、TTL 心跳键、serverType 集合，并在变更时通过频道发布通知
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Component
public class ServiceRegistryRepository {

    /**
     * 全局目录版本号在 Redis 中使用的 key
     *
     * @return java.lang.String Redis 键，对应 {@link com.mumu.game.core.redis.constants.RedisKey#SERVICE_REGISTRY_VERSION}
     */
    public String versionKey() {
        return RedisKey.SERVICE_REGISTRY_VERSION.buildKey();
    }

    /**
     * 已出现过的服务类型集合在 Redis 中使用的 key
     *
     * @return java.lang.String Redis 键，对应 {@link com.mumu.game.core.redis.constants.RedisKey#SERVICE_REGISTRY_TYPES}
     */
    public String typesKey() {
        return RedisKey.SERVICE_REGISTRY_TYPES.buildKey();
    }

    /**
     * 指定服务类型下全部实例 Hash 的 Redis key
     *
     * @param serviceTypeId 服务类型 id，与 {@link com.mumu.game.core.net.consts.ServiceType#getServiceId()} 一致
     * @return java.lang.String Redis 键，field 为 serverId，value 为实例 JSON
     */
    public String typeHashKey(int serviceTypeId) {
        return RedisKey.SERVICE_REGISTRY_TYPE_HASH.buildKey(serviceTypeId);
    }

    /**
     * 指定实例的 TTL 心跳 key
     *
     * @param serviceTypeId 服务类型 id
     * @param serverId      实例 id，与 Hash 中 field 一致
     * @return java.lang.String Redis 键，用于存活判断；过期后由 Gateway 对齐任务清理 Hash
     */
    public String heartbeatKey(int serviceTypeId, int serverId) {
        return RedisKey.SERVICE_REGISTRY_HEARTBEAT.buildKey(serviceTypeId, serverId);
    }

    /**
     * 读取当前全局目录版本号；Redis 中无该键或解析失败时返回 0
     *
     * @return long 全局版本，数值越大表示目录变更越新
     */
    public long getGlobalVersion() {
        // 版本键由 INCR 维护，首次不存在时按 0 处理，与「从未注册」语义一致
        String s = RedisUtil.get(versionKey());
        if (StringUtils.isBlank(s)) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            LogTopic.ACTION.error(e, "ServiceRegistryRepository.getGlobalVersion", "raw", s);
            return 0L;
        }
    }

    /**
     * 注册或更新实例：写入类型 Hash、维护类型 SET、写入带 TTL 的心跳、全局版本 INCR 并向 {@link com.mumu.game.core.redis.constants.RedisChannel#SERVICE_REGISTRY} 发布变更
     *
     * @param record              实例注册信息，不可为空且 serverId 不可为空
     * @param heartbeatTtlSeconds 心跳 key 过期时间（秒），应大于业务心跳间隔
     * @return long 递增后的全局版本号；参数非法时返回当前全局版本（不修改 Redis）
     */
    public long registerServer(RegisteredServerInfo record, int heartbeatTtlSeconds) {
        if (record == null || record.getServerId() <= 0) {
            return getGlobalVersion();
        }
        int serverId = record.getServerId();
        int serviceTypeId = record.getServiceTypeId();
        String json = JsonUtil.toJson(record);
        // 1) 写入该类型下的实例行；2) 记录出现过的 serverType；3) 独立心跳键带 TTL，供 Gateway 判断存活
        RedisUtil.hset(typeHashKey(serviceTypeId), String.valueOf(serverId), json);
        RedisUtil.sSet(typesKey(), String.valueOf(serviceTypeId));
        RedisUtil.set(heartbeatKey(serviceTypeId, serverId), 1, heartbeatTtlSeconds);
        // 目录发生实质变更，递增全局版本并通知订阅方（消息中带 serviceType 便于后续按类型拉取）
        long ver = RedisUtil.incr(versionKey());
        publishChange(ver, List.of(serviceTypeId));
        return ver;
    }

    /**
     * 仅刷新心跳 key 的 TTL，不修改实例 Hash、不递增全局版本，避免心跳周期触发全量拉取
     *
     * @param serviceTypeId       服务类型 id
     * @param serverId            实例 id
     * @param heartbeatTtlSeconds 心跳过期时间（秒）
     */
    public void keepHeartbeat(int serviceTypeId, int serverId, int heartbeatTtlSeconds) {
        if (serverId <= 0) {
            return;
        }
        // 仅刷新 TTL，不修改 Hash、不 INCR 版本，避免心跳风暴导致无意义的全量拉取
        RedisUtil.set(heartbeatKey(serviceTypeId, serverId), 1, heartbeatTtlSeconds);
    }

    /**
     * 主动下线：删除 Hash 中实例、删除心跳；若该类型下已无实例则从类型 SET 移除；递增全局版本并发布通知
     *
     * @param serviceTypeId 服务类型 id
     * @param serverId      实例 id
     * @return long 递增后的全局版本号；serverId 为空时返回当前全局版本（不修改 Redis）
     */
    public long unregisterServer(int serviceTypeId, int serverId) {
        if (serverId <= 0) {
            return getGlobalVersion();
        }
        String hKey = typeHashKey(serviceTypeId);
        RedisUtil.hdel(hKey, String.valueOf(serverId));
        RedisUtil.del(heartbeatKey(serviceTypeId, serverId));
        // 该类型下已无实例时从类型集合移除，避免拉全量时扫描空 Hash
        if (CollUtil.isEmpty(RedisUtil.hmget(hKey))) {
            RedisUtil.setRemove(typesKey(), String.valueOf(serviceTypeId));
        }
        long ver = RedisUtil.incr(versionKey());
        publishChange(ver, List.of(serviceTypeId));
        return ver;
    }

    /**
     * 拉取当前全量服务目录快照：先读全局版本，再按类型 SET 枚举各类型 Hash 并反序列化
     *
     * @return com.mumu.game.core.register.bo.ServiceRegistrySnapshot 目录快照，version 与 Redis 全局版本一致，byType 为按类型分组的实例映射
     */
    public ServiceRegistrySnapshot loadSnapshot() {
        // 先取版本号，再按 SET 中记录的类型逐个 HGETALL（类型集合由注册与 Gateway 对齐共同维护）
        long ver = getGlobalVersion();
        Map<Integer, Map<String, RegisteredServerInfo>> byType = new LinkedHashMap<>();
        Set<String> typeMembers = RedisUtil.sGet(typesKey());
        if (CollUtil.isEmpty(typeMembers)) {
            return ServiceRegistrySnapshot.builder().version(ver).serviceServerIdInfoMap(byType).build();
        }
        // TODO 可以优化使用 lua 脚本，批量获取所有 Hash
        List<Integer> sortedTypes = typeMembers.stream().map(Integer::parseInt).sorted().toList();
        for (Integer typeId : sortedTypes) {
            Map<String, RegisteredServerInfo> row = loadTypeRow(typeId);
            // 空 Hash 不入快照，与类型 SET 可能短暂不一致时仍以实例为准
            if (!row.isEmpty()) {
                byType.put(typeId, row);
            }
        }
        return ServiceRegistrySnapshot.builder().version(ver).serviceServerIdInfoMap(byType).build();
    }

    /**
     * 将某一类型 Hash 中的 JSON 行反序列化为内存对象，损坏或无法解析的条目跳过
     *
     * @param serviceTypeId 服务类型 id
     * @return java.util.Map&lt;java.lang.String, com.mumu.game.core.register.bo.RegisteredServerInfo&gt; 注册服务信息，key 为 serverId，value 为服务信息 {@link RegisteredServerInfo}
     */
    private Map<String, RegisteredServerInfo> loadTypeRow(int serviceTypeId) {
        Map<String, String> raw = RedisUtil.hmget(typeHashKey(serviceTypeId));
        Map<String, RegisteredServerInfo> m = new LinkedHashMap<>();
        if (CollUtil.isEmpty(raw)) {
            return m;
        }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            RegisteredServerInfo rec = JsonUtil.fromJson(e.getValue(), RegisteredServerInfo.class);
            if (rec != null) {
                m.put(e.getKey(), rec);
            }
        }
        return m;
    }

    /**
     * 供 Gateway 定时调用：扫描各类型 Hash，剔除「心跳 key 已不存在（TTL 过期）」的实例，必要时清理类型 SET；若有删除则 INCR 全局版本并发布通知
     *
     * @return com.mumu.game.core.register.ServiceRegistryRepository.PurgeResult 是否发生删除、递增后的版本号及受影响的 serviceTypeId 集合
     */
    public PurgeResult purgeInstancesWithoutHeartbeat() {
        Set<String> serviceTypeStrSet = RedisUtil.sGet(typesKey());
        if (CollUtil.isEmpty(serviceTypeStrSet)) {
            return PurgeResult.none();
        }
        Set<Integer> serviceTypeSet = new LinkedHashSet<>();
        for (String serviceTypeStr : serviceTypeStrSet) {
            int typeId;
            try {
                typeId = Integer.parseInt(serviceTypeStr);
            } catch (NumberFormatException e) {
                continue;
            }
            String hKey = typeHashKey(typeId);
            Map<String, String> serviceInfoMap = RedisUtil.hmget(hKey);
            // Hash 已空则清理类型 SET 中的脏项，保持元数据干净
            if (CollUtil.isEmpty(serviceInfoMap)) {
                RedisUtil.setRemove(typesKey(), serviceTypeStr);
                continue;
            }

            // 心跳键已过期（不存在）视为被动下线，从 Hash 中剔除
            List<Integer> serverIdSet = new ArrayList<>();
            for (String serverId : serviceInfoMap.keySet()) {
                int serverIdInt = Integer.parseInt(serverId);
                if (!RedisUtil.hasKey(heartbeatKey(typeId, serverIdInt))) {
                    serverIdSet.add(serverIdInt);
                }
            }
            if (serverIdSet.isEmpty()) {
                continue;
            }
            RedisUtil.hdel(hKey, serverIdSet);
            for (int serverId : serverIdSet) {
                RedisUtil.del(heartbeatKey(typeId, serverId));
            }
            serviceTypeSet.add(typeId);
            if (CollUtil.isEmpty(RedisUtil.hmget(hKey))) {
                RedisUtil.setRemove(typesKey(), serviceTypeStr);
            }
        }

        // 任一有删除则统一升一次版本并广播，合并本周期内多类型变更
        if (serviceTypeSet.isEmpty()) {
            return PurgeResult.none();
        }
        long version = RedisUtil.incr(versionKey());
        publishChange(version, new ArrayList<>(serviceTypeSet));
        return new PurgeResult(true, version, serviceTypeSet);
    }

    /**
     * 向注册中心频道发布 JSON 变更消息，供发现端做延迟拉取与版本比对
     *
     * @param version         递增后的全局版本号
     * @param serviceTypeIds 本次变更涉及的 serviceTypeId 列表
     */
    private void publishChange(long version, List<Integer> serviceTypeIds) {
        ServiceRegistryChangeMessage msg = new ServiceRegistryChangeMessage(version, serviceTypeIds);
        RedisChannel.SERVICE_REGISTRY.publish(msg);
    }

    /**
     * PurgeResult
     * Gateway 定时对齐任务执行结果：是否改动了目录、递增后的版本号及受影响的类型集合
     * @author liuzhen
     * @version 1.0.0 2026/5/5 15:56
     */
    @Data
    public static class PurgeResult {
        /**
         * 本次对齐是否实际删除了实例（若有则已触发版本递增与 Pub/Sub）
         */
        private final boolean changed;
        /**
         * 当 changed 为 true 时为递增后的全局版本；未变更时为 0（占位，业务应以 changed 为准）
         */
        private final long newVersion;
        /**
         * 本次被清理实例所属的服务类型 id 集合
         */
        private final Set<Integer> affectedTypeIds;

        /**
         * 构造「无任何删除」的结果
         *
         * @return com.mumu.game.core.register.ServiceRegistryRepository.PurgeResult changed=false，版本占位 0，受影响类型为空集
         */
        static PurgeResult none() {
            return new PurgeResult(false, 0L, Set.of());
        }

        /**
         * @param changed         是否发生删除
         * @param newVersion      递增后的全局版本（仅当 changed 为 true 时有意义）
         * @param affectedTypeIds 受影响的 serviceTypeId 集合
         */
        PurgeResult(boolean changed, long newVersion, Set<Integer> affectedTypeIds) {
            this.changed = changed;
            this.newVersion = newVersion;
            this.affectedTypeIds = affectedTypeIds;
        }
    }
}
