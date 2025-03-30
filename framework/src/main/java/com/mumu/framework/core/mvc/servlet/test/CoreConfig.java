/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.test;

import com.mumu.framework.core.cloud.ServiceType;
import com.mumu.framework.util.SpringContextUtils;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CoreConfig
 * 
 * @author liuzhen
 * @version 1.0.0 2025/3/16 16:30
 */
@Data
@Component
public class CoreConfig {

    /** 缓存延迟下线时间（分） */
    @Value("${model.cache-delay-offline:30}")
    private int cacheDelayOffline;
    /** 数据模型缓存时间 */
    @Value("${model.cache-day:7}")
    private int cacheDay;
    /** 数据模型缓存大小 */
    @Value("${model.cache-size:200000}")
    private int cacheSize;
    /** 服务器关服时间，默认开启=0 */
    @Getter
    public static volatile long serverClosingTime;
    /** 服务器组类型（频繁使用，定为静态变量） */
    @Getter
    private static ServiceType serviceType;
    /** 服务ID（频繁使用，定为静态变量） */
    @Getter
    private static int serverId;
    /** 玩家核心线程数 */
    private static int playerCoreSize;

    @Value("${net.group}")
    public void setServiceType(ServiceType serviceType) {
        CoreConfig.serviceType = serviceType;
    }

    @Value("${net.id}")
    public void setServerId(int serverId) {
        CoreConfig.serverId = serverId;
    }

    @Value("${net.thread.playerCorePoolSize:64}")
    public void setPlayerCoreSize(int playerCoreSize) {
        CoreConfig.playerCoreSize = playerCoreSize;
    }

    /** 计算路由线程id */
    public static int route(long sendId) {
        return (int)(sendId % playerCoreSize);
    }

    /** 服务器关服维护中(判断：服务器关服时间!=0，默认开启=0) */
    public static boolean isServerClosing() {
        return serverClosingTime != 0;
    }

    /** 开始停服 */
    public static void beginClosing() {
        serverClosingTime = System.currentTimeMillis();
    }

    /** 取消停服 */
    public static void cancelClosing() {
        serverClosingTime = 0;
    }

    public static CoreConfig self() {
        return SpringContextUtils.getBean(CoreConfig.class);
    }
}
