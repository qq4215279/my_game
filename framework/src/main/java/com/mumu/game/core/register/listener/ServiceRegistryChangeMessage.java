/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register.listener;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ServiceRegistryChangeMessage
 * 服务注册中心 Pub/Sub 通知载荷：携带递增后的全局版本号及本次涉及的 serviceType 列表，便于订阅方延迟拉取与后续按类型增量同步
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRegistryChangeMessage {

    /**
     * 全局目录版本号（递增后）
     * 与 Redis 中 {@link com.mumu.game.core.redis.constants.RedisKey#SERVICE_REGISTRY_VERSION} 对应值一致，用于发现端与本地版本比对
     */
    private long version;

    /**
     * 本次变更涉及的 serviceTypeId 列表
     * 订阅方可据此做按类型增量拉取（当前实现仍可全量拉取）；可能包含一种或多种类型
     */
    private List<Integer> serviceTypeIds = new ArrayList<>();
}
