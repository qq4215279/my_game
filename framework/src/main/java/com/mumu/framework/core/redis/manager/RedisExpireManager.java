package com.mumu.framework.core.redis.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.redis.RedisUtil;
import com.mumu.framework.core.redis.constants.RedisConstants;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * RedisExpireManager
 * RedisKey 过期时间控制器
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:42
 */
public class RedisExpireManager {
  /** K:过期Key V:过期Val */
  private static final Cache<String, Long> redisKeyExpireCache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1, TimeUnit.DAYS).build();

  /**
   * 临时redis的expire过滤功能，注意这个方法暂时只能用在过期时间超过1天的。如果过期时间很短的情况下不要用
   *
   * @param key key
   * @param expireSecond 多久秒后过期
   * @param consumer 具体redis的操作
   */
  public static void expire(String key, long expireSecond, BiConsumer<String, Long> consumer) {
    if (expireSecond <= 0) return;

    if (expireSecond < RedisConstants.ONE_DAY_SECOND) {
      consumer.accept(key, expireSecond);
      return;
    }
    Long res = redisKeyExpireCache.getIfPresent(key);
    if (null == res) {
      try {
        consumer.accept(key, expireSecond);
        redisKeyExpireCache.put(key, expireSecond);
      } catch (Exception e) {
        LogTopic.ACTION.error(
            e, "RedisExpireManager-expire", "key", key, "expireAtTime", expireSecond);
      }
    }
  }

  /** 设置缓存失效时间（有缓存的设置，规避同一个key重复多次设置过期时间） */
  public static void expire(String key, long expireSecond) {
    expire(key, expireSecond, RedisUtil::expire);
  }

  /**
   * 临时redis的expire过滤功能
   *
   * @param key key
   * @param expireAtTime 过期时间戳
   * @param consumer 具体redis的操作
   */
  public static void expireAt(String key, long expireAtTime, BiConsumer<String, Long> consumer) {
    Long res = redisKeyExpireCache.getIfPresent(key);
    // 过期设置值变化，才会去设置新的过期值
    if (null == res || res != expireAtTime) {
      try {
        consumer.accept(key, expireAtTime);
        redisKeyExpireCache.put(key, expireAtTime);
      } catch (Exception e) {
        LogTopic.ACTION.error(
            e, "RedisExpireManager-expireAt", "key", key, "expireAtTime", expireAtTime);
      }
    }
  }
}
