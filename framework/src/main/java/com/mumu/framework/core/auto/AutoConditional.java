/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.auto;

import com.mumu.framework.core.mvc.constants.ServiceType;

import java.util.Collection;

/**
 * AutoConditional
 * 条件接口
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:06
 */
public interface AutoConditional {

    /** 加载的服务 */
    default Collection<ServiceType> loadService() {
        return ServiceType.GAME_SERVERS;
    }

    /** 排序（小的优先执行） */
    default int order() {
        return 10;
    }
}
