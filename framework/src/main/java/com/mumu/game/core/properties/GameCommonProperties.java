package com.mumu.game.core.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * GameCommonProperties
 * 游戏通用配置注入
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:50
 */
@Data
@Component
public class GameCommonProperties {
    /** groovy 脚本是否开启 */
    @Value("${game.common.groovy.enable}")
    private String groovyEnable;
    /** groovy 脚本路径 */
    @Value("${game.common.groovy.groovyPath}")
    private String groovyPath;
}
