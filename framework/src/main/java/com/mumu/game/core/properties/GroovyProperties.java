package com.mumu.game.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * GroovyProperties
 * groovy配置注入
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:58
 */
@Data
@ConfigurationProperties(prefix = "game.common.groovy")
@Component
public class GroovyProperties {
    /** groovy 脚本是否开启 */
    private String groovyEnable;
    /** groovy 脚本路径 */
    private String groovyPath;
}
