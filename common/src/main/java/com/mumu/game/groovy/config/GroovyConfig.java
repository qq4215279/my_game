package com.mumu.game.groovy.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.hutool.core.io.FileUtil;
import groovy.util.GroovyScriptEngine;
import lombok.extern.slf4j.Slf4j;

/**
 * GroovyConfig
 * Groovy配置
 * @author liuzhen
 * @version 1.0.0 2026/8/20 09:34
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "groovy", name = "enable", havingValue = "true")
public class GroovyConfig {

  @Value("${groovy.groovyPath:scripts/groovy}")
  private String groovyPath;

  @Bean
  public GroovyScriptEngine getGroovyScriptEngine() throws IOException {
    log.info(
        "GroovyConfig load...! groovyPath: {}, file: {}", groovyPath, FileUtil.mkdir(groovyPath));
    return new GroovyScriptEngine(groovyPath);
  }
}
