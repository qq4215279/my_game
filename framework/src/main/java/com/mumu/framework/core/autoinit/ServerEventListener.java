/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.autoinit;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mumu.common.constants.CoreConstants;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.thread.ScheduledExecutorUtil;

import cn.hutool.core.lang.ClassScanner;
import jakarta.annotation.Resource;

/**
 * ServerEventListener
 * 服务容器事件触发类（用于初始化和销毁部分组件）
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:08
 */
@Component
public class ServerEventListener implements SmartInitializingSingleton {
    /** 自动初始化 */
    @Resource
    AutoInitManager autoInitManager;

    @Override
    public void afterSingletonsInstantiated() {
        AutoInitManager.CLASSES = new ClassScanner(CoreConstants.PACKAGE, null).scan();
        LogTopic.ACTION.info("单例Bean初始化完成事件 ServerEventListener...", "scanClass", AutoInitManager.CLASSES.size());

        autoInitManager.load();
    }

    /** 容器关闭事件 */
    @Order(10)
    @EventListener(ContextClosedEvent.class)
    public void onClosed(ContextClosedEvent event) {
        LogTopic.ACTION.info("容器关闭 ServerEventListener...");
        ScheduledExecutorUtil.destroy();
    }
}
