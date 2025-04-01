/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.autoinit;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mumu.framework.core.autoinit.enums.AutoInitModule;
import com.mumu.framework.core.cloud.ServiceType;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.util.SpringContextUtils;

import cn.hutool.core.util.StrUtil;

/**
 * AutoInitManager 初始化管理器，会扫描全部 {@link AutoInitEvent} 实现，并执行其初始化方法
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:11
 */
@Component
public class AutoInitManager {
    Map<AutoInitModule, Set<AutoInitEvent>> autoInitMap = Collections.emptyMap();

    public static Set<Class<?>> CLASSES = Collections.emptySet();

    static final Comparator<AutoInitEvent> COMPARATOR =
        Comparator.comparingInt(AutoInitEvent::order).thenComparingInt(Object::hashCode);

    /**
     * load
     * @return void
     */
    public void load() {
        Map<AutoInitModule, Set<AutoInitEvent>> autoInitMap = Maps.newHashMap();
        Map<String, AutoInitEvent> eventMap = SpringContextUtils.getBeansOfType(AutoInitEvent.class);

        ServiceType curr = ServiceType.curr();
        for (AutoInitEvent event : eventMap.values()) {
            if (event.loadService().contains(curr)) {
                autoInitMap.computeIfAbsent(event.getInitGroup(), group -> Sets.newTreeSet(COMPARATOR)).add(event);
            }
        }

        this.autoInitMap = autoInitMap;
        LogTopic.ACTION.info("AutoInitManager.load",
            StrUtil.format("扫描到[AutoInitEvent]实现类[{}]个, 解析模块[{}]个!", eventMap.size(), autoInitMap.size()));
        // 考虑是否在此执行初始化
        batchInit();
    }

    private void batchInit() {
        for (AutoInitModule module : AutoInitModule.values()) {
            autoInitMap.getOrDefault(module, Collections.emptySet()).forEach(this::execute);
        }
        LogTopic.ACTION.info("全部[AutoInitEvent]执行完成!");
    }

    private void execute(AutoInitEvent event) {
        try {
            event.autoInit();
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "AutoInitManager execute", "module", event.getInitGroup(), "clazz",
                event.getClass());
        }
    }
}
