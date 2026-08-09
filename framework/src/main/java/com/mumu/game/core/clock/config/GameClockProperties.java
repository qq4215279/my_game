/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * GameClockProperties
 * 游戏时间配置
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
@Data
@Component
@ConfigurationProperties(prefix = "game.clock")
public class GameClockProperties {

    /** 是否允许在当前进程修改游戏时间和玩家时间 */
    private boolean gmEnabled;
}
