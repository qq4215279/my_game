package com.mumu.framework.core.redis.config;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.redis.chanel.RedisChannelAdapter;
import com.mumu.framework.core.redis.chanel.RedisChannelListener;
import com.mumu.framework.core.redis.constants.RedisChannel;
import com.mumu.framework.core.redis.constants.RedisLuaScript;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * RedisConfiguration
 * redis配置工具
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:35
 */
@Configuration
public class RedisConfiguration {
  private static final LogTopic log = LogTopic.ACTION;

  RedisConnectionFactory redisFactory;

  public RedisConfiguration(RedisConnectionFactory redisFactory) {
    this.redisFactory = redisFactory;
    preloadScript();
  }

  /** 预加载 Lua脚本 */
  private void preloadScript() {
    log.info("加载Redis配置，lua脚本预加载...");
    try (RedisConnection connection = redisFactory.getConnection()) {
      for (RedisLuaScript script : RedisLuaScript.values()) {
        String sha = connection.scriptingCommands().scriptLoad(script.getScriptText().getBytes());
        log.info("preloadScript", "LuaScrip", script, "sha",sha);
      }
    }
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    log.info("加载Redis配置，RedisTemplate init...");
    // 创建Template
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    // 设置连接工厂
    redisTemplate.setConnectionFactory(redisFactory);
    // key 和 hashKey 采用 string 序列化
    redisTemplate.setKeySerializer(RedisSerializer.string());
    redisTemplate.setHashKeySerializer(RedisSerializer.string());
    // value 和 hashValue 采用 JSON 序列化
    GenericJackson2JsonRedisSerializer jsonRedisSerializer =
        new GenericJackson2JsonRedisSerializer();
    redisTemplate.setValueSerializer(jsonRedisSerializer);
    redisTemplate.setHashValueSerializer(jsonRedisSerializer);
    return redisTemplate;
  }

  @Bean
  public StringRedisTemplate stringRedisTemplate() {
    log.info("加载Redis配置，StringRedisTemplate init...");
    StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
    stringRedisTemplate.setConnectionFactory(redisFactory);
    return stringRedisTemplate;
  }

  @Bean
  public RedisMessageListenerContainer redisContainer(List<RedisChannelListener> listeners) {
    log.info("init Redis 发布订阅器...  channel", "size", listeners.size());
    if (listeners.isEmpty()) {
      return null;
    }
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisFactory);

    // 解析订阅渠道
    Map<RedisChannel, List<RedisChannelListener>> channelMap = Maps.newHashMap();
    for (RedisChannelListener listener : listeners) {
      RedisChannel channel = listener.subscribeChannel();
      Assert.notNull(channel, "Redis 订阅渠道不能为空! {}", listener.getClass());
      channelMap.computeIfAbsent(channel, k -> Lists.newArrayList()).add(listener);
    }
    // 开始注册订阅
    channelMap.forEach(
        (channel, ls) -> {
          container.addMessageListener(
              new MessageListenerAdapter(new RedisChannelAdapter(ls)),
              new ChannelTopic(channel.getChannel()));
          log.info(StrUtil.format("Redis 订阅渠道: {}, listener: {} {}", channel, ls.size(), ls));
        });
    return container;
  }
}
