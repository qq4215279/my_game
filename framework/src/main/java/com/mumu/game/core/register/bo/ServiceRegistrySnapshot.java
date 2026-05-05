/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register.bo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ServiceRegistrySnapshot
 * 某次从 Redis 拉取得到的全局服务目录快照，结构为 serviceTypeId → serverId → 实例信息
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRegistrySnapshot {

    /**
     * 快照对应的全局版本号
     * 应与拉取时刻 Redis 中全局版本一致，用于与本地已处理版本比较
     */
    private long version;

    /**
     * 按服务类型分组的实例目录
     * 外层 key：serviceTypeId；内层 key：serverId；value：该实例注册信息
     */
    @Builder.Default
    private Map<Integer, Map<String, RegisteredServerInfo>> serviceServerIdInfoMap = new LinkedHashMap<>();

    /**
     * 返回只读嵌套 Map 视图，避免业务监听方直接改动内部缓存结构
     * @return 外层 key 为 serviceTypeId，内层 key 为 serverId，value 为 {@link RegisteredServerInfo}
     */
    public Map<Integer, Map<String, RegisteredServerInfo>> getServiceMapByClone() {
        Map<Integer, Map<String, RegisteredServerInfo>> outer = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<String, RegisteredServerInfo>> e : serviceServerIdInfoMap.entrySet()) {
            // 内层按 serverId 拷贝后再包一层不可变，防止监听方修改条目集合
            outer.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(outer);
    }
}
