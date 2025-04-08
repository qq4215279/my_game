/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.constants;

import java.util.List;

import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.config.CoreConfig;

import lombok.Getter;

/**
 * ServiceType
 * 服务id组
 * @author liuzhen
 * @version 1.0.0 2025/3/16 20:08
 */
@Getter
public enum ServiceType {
    /** 全部服 */
    ALL(-1),
    /** 网关服 */
    GATE(0),
    /** 大厅服 */
    WORLD(1),
    /** 游戏服 */
    GAME(2),
    /** 聊天服 */
    CHAT(3);

    /** 服务id */
    private final int serviceId;

    ServiceType(int serviceId) {
        this.serviceId = serviceId;
    }

    /**
     *
     * @param serviceId serviceId
     * @return com.mumu.framework.core.cloud.ServiceType
     * @author liuzhen
     * @date 2025/3/28 23:05
     */
    public static ServiceType getServiceType(int serviceId) {
        for (ServiceType serviceType : ServiceType.values()) {
            if (serviceType.getServiceId() == serviceId) {
                return serviceType;
            }
        }

        LogTopic.NET.error("getServiceType.fail", "not find", "serviceId", serviceId);
        return null;
    }



    /** 判断是当前服 */
    public boolean inMyself() {
        return curr() == this;
    }

    /** 判断不是当前服 */
    public boolean notMyself() {
        return !inMyself();
    }

    /** 玩家所在的服务组类型，优先级由高到低 */
    public static final List<ServiceType> PLAYER_ON_GROUPS = List.of(GAME, WORLD, GATE, CHAT);

    /** 游戏相关业务服 */
    public static final List<ServiceType> GAME_SERVERS = List.of(GAME, WORLD, CHAT);

    /**
     * 获取当前服务类型
     */
    public static ServiceType curr() {
        return CoreConfig.getServiceType();
    }
}
