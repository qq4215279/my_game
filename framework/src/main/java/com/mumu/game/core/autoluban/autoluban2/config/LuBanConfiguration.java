package com.mumu.game.core.autoluban.autoluban2.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cxx.luban.lubanconfigclient.ILubanConfigClient;
import com.cxx.luban.lubanconfigclient.LubanConfigClientBuilder;
import com.cxx.luban.lubanconfigclient.internal.LubanConstants;
import com.game.framework.core.log.LogTopic;

/** 鲁班自动装配 @Date: 2021/7/8 17:21 @Author: xu.hai */
@Configuration
@EnableConfigurationProperties(LuBanProperties.class)
@ConditionalOnProperty(prefix = LuBanProperties.LUBAN_PREFIX, name = "enable", havingValue = "true")
public class LuBanConfiguration {

  @Bean
  public ILubanConfigClient luBanClient(LuBanProperties properties) {
    LogTopic log = LogTopic.ACTION;

    // 1、获取客户端实例
    ILubanConfigClient client =
        LubanConfigClientBuilder.build(
            properties.isRemote(),
            properties.getPort(),
            properties.getConfigEntityPackageNames(),
            properties.getConfigFileCachePath(),
            properties.getEndpoint(),
            properties.getSecretAccessKey(),
            // 设置内部事件日志消费函数，也可以设置null，那么就是往控制台打了
            eventLog -> {
              if (eventLog.getLevel().equals(LubanConstants.LOG_LEVEL_ERROR)) {
                log.error("luban-config-log-event", eventLog.getLevel(), eventLog.getContent(), eventLog.getCause());
              } else {
                // log.info("luban-config-log-event", eventLog.getLevel(), eventLog.getContent(), eventLog.getCause());
              }
            });
    // 2、执行初始化(扫描class文件)
    client.initialize();
    log.info("luban service initialize success!", properties);
    return client;
  }
}
