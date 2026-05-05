/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.auto;

import java.util.Collection;

import com.mumu.game.core.net.consts.ServiceType;

/**
 * AutoConditional
 * 条件接口
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:06
 */
public interface AutoConditional {

    /** 加载的服务 */
    default Collection<ServiceType> loadService() {
        return ServiceType.PLAYER_ON_GROUPS;
    }

    /** 排序（小的优先执行） */
    default int order() {
        return 10;
    }
}
