/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.register;

import com.mumu.game.core.register.bo.ServiceRegistrySnapshot;

/**
 * ServiceRegistryListener
 * 本地服务目录更新回调接口，在发现客户端完成版本校验与全量拉取后触发，可用于驱动 Netty 建连、断连等
 * @author liuzhen
 * @version 1.0.0 2026/5/5 15:56
 */
@FunctionalInterface
public interface ServiceRegistryListener {

    /**
     * 当 Redis 全局版本高于本地已处理版本并完成快照加载后调用，业务可在此根据最新目录维护 Netty 连接等
     * @param snapshot 最新目录快照；若需只读访问可优先使用 {@link ServiceRegistrySnapshot#getServiceMapByClone()}
     */
    void onSnapshotUpdated(ServiceRegistrySnapshot snapshot);
}
