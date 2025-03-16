package com.mumu.framework.core.redis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mumu.common.util2.JsonUtil;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.redis.constants.RedisLuaScript;
import com.mumu.framework.core.redis.constants.ScoreInfo;
import com.mumu.framework.core.redis.manager.RedisExpireManager;
import com.mumu.framework.util2.CovertUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

/**
 * RedisUtil
 * Redis 操作工具类
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:37
 */
@Component
@SuppressWarnings("All")
public class RedisUtil {

  static StringRedisTemplate stringRedisTemplate;
  static RedisConnectionFactory factory;

  @Autowired
  public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
    RedisUtil.stringRedisTemplate = stringRedisTemplate;
  }

  @Autowired
  public void setFactory(RedisConnectionFactory factory) {
    RedisUtil.factory = factory;
  }

  // ============================ 扩展工具 =============================

  /** 执行redis操作 */
  public static <T> T doCmd(
      String handler, T defValue, Function<StringRedisTemplate, T> func, Object... params) {
    try {
      T res = func.apply(stringRedisTemplate);
      return res == null ? defValue : res;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, handler, params);
      return defValue;
    }
  }

  /** 管道批量执行 */
  public static List<Object> executePipelined(Consumer<StringRedisConnection> consumer) {
    return stringRedisTemplate.executePipelined(
        (RedisCallback<Object>)
            connection -> {
              consumer.accept((StringRedisConnection) connection);
              return null;
            });
  }

  /** 执行lua脚本 */
  public static <T> T executeLua(RedisLuaScript luaScript, List<String> keys, Object... args) {
    return stringRedisTemplate.execute(luaScript.getScript(), keys, args);
  }

  /** 获取 Redis 锁 */
  public static boolean tryLock(String key, int expire) {
    return tryLock(key, "1", expire);
  }

  /** 获取 Redis 锁 */
  public static boolean tryLock(String key, String val, int expire) {
    try {
      Boolean res =
          stringRedisTemplate.opsForValue().setIfAbsent(key, val, expire, TimeUnit.SECONDS);
      return Boolean.TRUE.equals(res);
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "tryLock", "key", key, "val", val, "expire", expire);
      return false;
    }
  }

  /** 批量获取匹配的全部 key（eachCount 每次扫描的数量） */
  public static List<String> batchGetKey(String keyPattern, int eachCount) {
    try (RedisConnection connection = factory.getConnection()) {
      ScanOptions options = ScanOptions.scanOptions().match(keyPattern).count(eachCount).build();
      try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
        List<String> batchKeys = Lists.newArrayList();
        while (cursor.hasNext()) {
          batchKeys.add(new String(cursor.next()));
        }
        return batchKeys;
      }
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "batchGetKey", "keyPattern", keyPattern, "eachCount", eachCount);
      return Collections.emptyList();
    }
  }

  /** 批量获取多个key的值 */
  public static <T> Map<String, T> batchGet(String keyPattern, int eachCount, Class<T> clazz) {
    return batchGet(keyPattern, eachCount, json -> JsonUtil.fromJson(json, clazz));
  }

  /** 批量获取多个key的值 */
  public static <T> Map<String, T> batchGet(
      String keyPattern, int eachCount, Function<String, T> func) {
    List<String> batchKeys = batchGetKey(keyPattern, eachCount);
    if (CollectionUtil.isEmpty(batchKeys)) {
      return Collections.emptyMap();
    }
    try {
      Map<String, T> result = Maps.newHashMap();
      for (List<String> keys : Lists.partition(batchKeys, eachCount)) {
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        if (CollectionUtil.isEmpty(values)) continue;
        for (int i = 0; i < keys.size() && i < values.size(); i++) {
          result.put(keys.get(i), func.apply(values.get(i)));
        }
      }
      return result;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "batchGet", "keyPattern", keyPattern, "eachCount", eachCount);
      return Collections.emptyMap();
    }
  }

  /** 发布消息 */
  public static void publish(String channel, String message) {
    stringRedisTemplate.convertAndSend(channel, message);
  }

  /** 发布消息 */
  public static void publish(String channel, byte[] message) {
    stringRedisTemplate.execute(
        connection -> connection.publish(channel.getBytes(StandardCharsets.UTF_8), message), true);
  }

  /** 发布消息 */
  public static void publishJson(String channel, Object message) {
    publish(channel, JsonUtil.toJson(message));
  }

  // ============================ 基础方法 =============================

  /**
   * 判断 key 是否存在
   *
   * @param key 键
   * @return 如果存在 key 则返回 true，否则返回 false
   */
  public static Boolean exists(String key) {
    try {
      return stringRedisTemplate.hasKey(key);
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "exists", "key", key);
      return false;
    }
  }

  /**
   * 获取 Key 的类型
   *
   * @param key 键
   */
  public static String type(String key) {
    DataType dataType = stringRedisTemplate.type(key);
    assert dataType != null;
    return dataType.code();
  }

  /**
   * 指定缓存失效时间
   *
   * @param key 键
   * @param time 时间(秒)
   * @return true 成功 false 失败
   */
  public static boolean expire(String key, long time) {
    try {
      if (time > 0) {
        stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);
      }
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "expire", "key", key, "time", time);
      return false;
    }
  }

  /**
   * 根据key 获取过期时间
   *
   * @param key 键 不能为null
   * @return 时间(秒) 返回0代表为永久有效
   */
  public static long getExpire(String key) {
    try {
      return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "getExpire", "key", key);
      return 0;
    }
  }

  /**
   * 判断key是否存在
   *
   * @param key 键
   * @return true 存在 false不存在
   */
  public static boolean hasKey(String key) {
    try {
      return stringRedisTemplate.hasKey(key);
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "hasKey", "key", key);
      return false;
    }
  }

  /**
   * 删除缓存
   *
   * @param keys 可以传一个值 或多个
   */
  public static Long del(String... keys) {
    if (keys.length == 0) {
      return 0L;
    }
    return stringRedisTemplate.delete(Arrays.asList(keys));
  }

  // ============================String=============================
  /** 设置Redis位图 */
  public static void setBit(String key, long offset) {
    setBit(key, offset, true);
  }

  /** 设置Redis位图 */
  public static void setBit(String key, long offset, boolean value) {
    doCmd(
        "setBit",
        StringUtils.EMPTY,
        t -> t.opsForValue().setBit(key, offset, value),
        key,
        offset,
        value);
  }

  /** 获取redis位图 */
  public static boolean getBit(String key, long offset) {
    return doCmd("getBit", false, t -> t.opsForValue().getBit(key, offset), key, offset);
  }

  /** get */
  public static <T> T get(String key, Class<T> clazz) {
    return get(key, json -> JsonUtil.fromJson(json, clazz));
  }

  /** get */
  public static <T> T get(String key, Function<String, T> func) {
    return func.apply(get(key));
  }

  /** get */
  public static String get(String key) {
    return doCmd("get", StringUtils.EMPTY, t -> t.opsForValue().get(key), key);
  }

  /** 普通缓存放入 */
  public static boolean set(String key, Object value) {
    try {
      stringRedisTemplate.opsForValue().set(key, JsonUtil.toJson(value));
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "set", "key", key, "value", value);
      return false;
    }
  }

  /**
   * 普通缓存放入并设置时间
   *
   * @param key 键
   * @param value 值
   * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
   * @return true成功 false 失败
   */
  public static boolean set(String key, Object value, long time) {
    try {
      if (time > 0) {
        stringRedisTemplate.opsForValue().set(key, JsonUtil.toJson(value), time, TimeUnit.SECONDS);
      } else {
        set(key, value);
      }
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "set", "key", key, "value", value, "time", time);
      return false;
    }
  }

  /** 递增 */
  public static long incr(String key, long offset) {
    return doCmd("incr", 0L, t -> t.opsForValue().increment(key, offset), key, offset);
  }

  /** 递增 */
  public static long incr(String key) {
    return incr(key, 1);
  }

  /** 递减 */
  public static long decr(String key) {
    return incr(key, -1);
  }

  // ================================ Map =================================

  /** hash get int */
  public static Integer hGetInt(String key, String item) {
    Integer i = hGet(key, item, Integer::parseInt);
    return i == null ? 0 : i;
  }

  /** hash get */
  public static <T> T hGet(String key, String item, Function<String, T> func) {
    Object val = stringRedisTemplate.opsForHash().get(key, item);
    if (val == null) return null;
    return func.apply(val.toString());
  }

  /** 获取 hashKey对应的所有键值 */
  public static Map<String, String> hmget(String key) {
    return hmget(key, String::valueOf);
  }

  /** 获取 hashKey对应的所有键值 */
  public static <V> Map<String, V> hmget(String key, Function<String, V> vFunc) {
    return hmget(key, String::valueOf, vFunc);
  }

  /** 获取 hashKey对应的所有键值（注：hash数据较大时，优先使用 batchHGet） */
  public static <K, V> Map<K, V> hmget(
      String key, Function<String, K> kFunc, Function<String, V> vFunc) {
    try {
      Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
      if (CollUtil.isEmpty(entries)) return Collections.emptyMap();
      return entries.entrySet().stream()
          .collect(
              Collectors.toMap(
                  e -> kFunc.apply(e.getKey().toString()),
                  e -> vFunc.apply(e.getValue().toString())));
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "hmget", "key", key);
      return Collections.emptyMap();
    }
  }

  /** 批量获取 hashKey的多个值（分页获取） */
  public static <V> List<V> batchHGetValues(
      String key, String fieldPattern, int eachCount, Function<String, V> vFunc) {
    return Lists.newArrayList(batchHGet(key, fieldPattern, eachCount, k -> k, vFunc).values());
  }

  /** 批量获取 hashKey的多个值（分页获取） */
  public static <K, V> Map<K, V> batchHGet(
      String key,
      String fieldPattern,
      int eachCount,
      Function<String, K> kFunc,
      Function<String, V> vFunc) {
    try (RedisConnection conn = factory.getConnection()) {
      ScanOptions options = ScanOptions.scanOptions().match(fieldPattern).count(eachCount).build();
      try (Cursor<Entry<byte[], byte[]>> cursor =
          conn.hashCommands().hScan(key.getBytes(StandardCharsets.UTF_8), options)) {
        Map<K, V> resMap = Maps.newHashMap();
        while (cursor.hasNext()) {
          Entry<byte[], byte[]> entry = cursor.next();
          resMap.put(
              kFunc.apply(new String(entry.getKey(), StandardCharsets.UTF_8)),
              vFunc.apply(new String(entry.getValue(), StandardCharsets.UTF_8)));
        }
        return resMap;
      }
    } catch (Exception e) {
      LogTopic.ACTION.error(
          e, "batchGetKey", "key", key, "fieldPattern", fieldPattern, "eachCount", eachCount);
      return Collections.emptyMap();
    }
  }

  /** 批量设置 hashKey的多个值（分页设置） */
  public static void batchHSet(String key, Map<String, String> map, int eachCount) {
    CovertUtil.partition(map, eachCount).forEach(eachMap -> hmset(key, eachMap));
  }

  /**
   * HashSet
   *
   * @param key 键
   * @param map 对应多个键值
   * @return true 成功 false 失败
   */
  public static boolean hmset(String key, Map<String, String> map) {
    try {
      stringRedisTemplate.opsForHash().putAll(key, map);
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "hmset", "key", key, "map", map);
      return false;
    }
  }

  /**
   * HashSet 并设置时间
   *
   * @param key 键
   * @param map 对应多个键值
   * @param time 时间(秒)
   * @return true成功 false失败
   */
  public static boolean hmset(String key, Map<String, Object> map, long time) {
    try {
      stringRedisTemplate.opsForHash().putAll(key, map);
      if (time > 0) {
        expire(key, time);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 向一张hash表中放入数据,如果不存在将创建
   *
   * @param key 键
   * @param item 项
   * @param value 值
   * @return true 成功 false失败
   */
  public static boolean hset(String key, String item, Object value) {
    return hset(key, item, JsonUtil.toJson(value));
  }

  public static boolean hset(String key, String item, String value) {
    return doCmd(
        "hset",
        false,
        t -> {
          t.opsForHash().put(key, item, value);
          return true;
        },
        key,
        item,
        value);
  }

  /**
   * 向一张hash表中放入数据,如果不存在将创建
   *
   * @param key 键
   * @param item 项
   * @param value 值
   * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
   * @return true 成功 false失败
   */
  public static boolean hset(String key, String item, Object value, long time) {
    try {
      stringRedisTemplate.opsForHash().put(key, item, JsonUtil.toJson(value));
      if (time > 0) {
        expire(key, time);
      }
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "hset", "key", key, "item", item, "value", value, "time", time);
      return false;
    }
  }

  /**
   * 删除hash表中的值
   *
   * @param key 键 不能为null
   * @param item 项 可以使多个 不能为null
   */
  public static void hdel(String key, Object... item) {
    stringRedisTemplate.opsForHash().delete(key, item);
  }

  /** 批量删除 hash表中值 */
  public static void hdel(String key, Collection<String> fields) {
    if (CollUtil.isNotEmpty(fields)) hdel(key, fields.toArray());
  }

  /**
   * 判断hash表中是否有该项的值
   *
   * @param key 键 不能为null
   * @param item 项 不能为null
   * @return true 存在 false不存在
   */
  public static boolean hHasKey(String key, String item) {
    return stringRedisTemplate.opsForHash().hasKey(key, item);
  }

  /** hash递增 如果不存在,就会创建一个 并把新增后的值返回 */
  public static long hincr(String key, String item, long by) {
    return doCmd("hincr", 0L, t -> t.opsForHash().increment(key, item, by), key, item, by);
  }

  /** hash递减 */
  public static long hdecr(String key, String item) {
    return hincr(key, item, -1);
  }

  // ============================set=============================

  /** 根据key获取Set中的所有值 */
  public static Set<String> sGet(String key) {
    return doCmd("sGet", Collections.emptySet(), tmp -> tmp.opsForSet().members(key), key);
  }

  /**
   * 判断指定set中是否存在value
   *
   * @param key 键
   * @param value 值
   * @return true 存在 false不存在
   */
  public static boolean sHasKey(String key, Object value) {
    return doCmd(
        "sHasKey", false, tmp -> tmp.opsForSet().isMember(key, String.valueOf(value)), key, value);
  }

  /** 取出并移除set中N个元素 */
  public static List<String> sPop(String key, long count) {
    return doCmd(
        "sPop", Collections.emptyList(), tmp -> tmp.opsForSet().pop(key, count), key, count);
  }

  /**
   * 将数据放入set缓存
   *
   * @param key 键
   * @param values 值 可以是多个
   * @return 成功个数
   */
  public static long sSet(String key, String... values) {
    try {
      return stringRedisTemplate.opsForSet().add(key, values);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * 将set数据放入缓存
   *
   * @param key 键
   * @param time 时间(秒)
   * @param values 值 可以是多个
   * @return 成功个数
   */
  public static long sSetAndTime(String key, long time, String... values) {
    try {
      Long count = stringRedisTemplate.opsForSet().add(key, values);
      if (time > 0) expire(key, time);

      return count;

    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * 获取set缓存的长度
   *
   * @param key 键
   * @return
   */
  public static long sGetSetSize(String key) {
    try {
      return stringRedisTemplate.opsForSet().size(key);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * 移除值为value的
   *
   * @param key 键
   * @param values 值 可以是多个
   * @return 移除的个数
   */
  public static long setRemove(String key, Object... values) {
    try {
      Long count = stringRedisTemplate.opsForSet().remove(key, values);
      return count;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  // ===============================list=================================

  /**
   * 获取list缓存的内容
   *
   * @param key 键
   * @param start 开始
   * @param end 结束 0 到 -1代表所有值
   * @return
   */
  public static List<String> lGet(String key, long start, long end) {
    try {
      return stringRedisTemplate.opsForList().range(key, start, end);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 获取list缓存的内容
   *
   * @param key 键
   * @param start 开始
   * @param end 结束 0 到 -1代表所有值
   * @return
   */
  public static List<String> getList(String key, long start, long end) {
    try {
      return stringRedisTemplate.opsForList().range(key, start, end);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 获取list缓存的长度
   *
   * @param key 键
   * @return
   */
  public static long lGetListSize(String key) {
    try {
      return stringRedisTemplate.opsForList().size(key);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * 通过索引 获取list中的值
   *
   * @param key 键
   * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
   * @return
   */
  public static Object lGetIndex(String key, long index) {
    try {
      return stringRedisTemplate.opsForList().index(key, index);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 将list放入缓存
   *
   * @param key 键
   * @param value 值
   * @return
   */
  public static boolean lSet(String key, String value) {
    try {
      stringRedisTemplate.opsForList().rightPush(key, value);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 将list放入缓存
   *
   * @param key 键
   * @param value 值
   * @param time 时间(秒)
   */
  public static boolean lSet(String key, String value, long time) {
    try {
      stringRedisTemplate.opsForList().rightPush(key, value);
      if (time > 0) expire(key, time);
      return true;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "lSet", "key", key, "value", value, "time", time);
      return false;
    }
  }

  /**
   * 将list放入缓存
   *
   * @param key 键
   * @param value 值
   * @return
   */
  public static boolean lSet(String key, List<String> value) {
    try {
      stringRedisTemplate.opsForList().rightPushAll(key, value);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 将list放入缓存
   *
   * @param key 键
   * @param value 值
   * @param time 时间(秒)
   * @return
   */
  public static boolean lSet(String key, List<String> value, long time) {
    try {
      stringRedisTemplate.opsForList().rightPushAll(key, value);
      if (time > 0) expire(key, time);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 根据索引修改list中的某条数据
   *
   * @param key 键
   * @param index 索引
   * @param value 值
   * @return
   */
  public static boolean lUpdateIndex(String key, long index, String value) {
    try {
      stringRedisTemplate.opsForList().set(key, index, value);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 移除N个值为value
   *
   * @param key 键
   * @param count 移除多少个
   * @param value 值
   * @return 移除的个数
   */
  public static long lRemove(String key, long count, Object value) {

    try {
      Long remove = stringRedisTemplate.opsForList().remove(key, count, value);
      return remove;

    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  // =============================== zset =================================
  /** 获取积分 */
  public static long getScore(String key, long id) {
    return doCmd(
        "getScore",
        0L,
        t -> {
          Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(id));
          return score != null ? (long) Math.ceil(score) : 0;
        },
        key,
        id);
  }

  /** 获取排名（第一名为1，-1表示未查到） */
  public static long getRank(String key, long id) {
    return doCmd(
        "getRank",
        -1L,
        t ->
            Optional.ofNullable(t.opsForZSet().reverseRank(key, String.valueOf(id)))
                .map(r -> r + 1)
                .orElse(-1L),
        key,
        id);
  }

  /** 批量获取指定玩家积分和排名 */
  public static List<ScoreInfo> getRankWithScore(String key, Collection<Long> ids) {
    return getRankWithScore(key, ids.toArray(Object[]::new));
  }

  /** 批量获取指定玩家积分和排名 */
  public static List<ScoreInfo> getRankWithScore(String key, Object... ids) {
    String res = RedisLuaScript.ZSET_GET_RANK_WITH_SCORE.executeLua(List.of(key), ids);
    return JsonUtil.fromJsonList(res, ScoreInfo.class);
  }

  /** 覆盖玩家积分 */
  public static boolean zadd(String key, long id, long score) {
    return doCmd(
        "zadd", false, t -> t.opsForZSet().add(key, String.valueOf(id), score), key, id, score);
  }

  /** 增加玩家积分 */
  public static long zIncr(String key, long id, long score) {
    return zIncr(key, id, score, false);
  }

  /** 增加玩家积分 removeNegative-true，表示积分小于0时移除 */
  public static long zIncr(String key, long id, long score, boolean removeNegative) {
    // return doCmd(
    //     "zaddIncr",
    //     0d,
    //     t -> t.opsForZSet().incrementScore(key, String.valueOf(id), (double) score),
    //     key,
    //     id,
    //     score);
    return RedisLuaScript.ZSCORE_UPDATE_BY_TIME.executeLua(
        List.of(
            key,
            String.valueOf(id),
            String.valueOf(score),
            String.valueOf(System.currentTimeMillis() / 1000),
            String.valueOf(removeNegative)));
  }

  /** 增加玩家积分 */
  public static void incrWithExpire(String key, long id, long score, int expireTime) {
    incrWithExpire(key, id, score, expireTime, false);
  }

  /** 增加玩家积分 removeNegative-true，表示积分小于0时移除 */
  public static void incrWithExpire(
      String key, long id, long score, int expireTime, boolean removeNegative) {
    executePipelined(
        connection -> {
          RedisLuaScript.ZSCORE_UPDATE_BY_TIME.executeLua(
              connection,
              key,
              String.valueOf(id),
              String.valueOf(score),
              String.valueOf(System.currentTimeMillis() / 1000),
              String.valueOf(removeNegative));
          if (expireTime > 0) RedisExpireManager.expire(key, expireTime, connection::expire);
        });
  }

  /** 移除指定玩家 */
  public static long zremove(String key, long id) {
    return doCmd("zremove", 0L, t -> t.opsForZSet().remove(key, String.valueOf(id)), key, id);
  }

  /** 获取榜单人数 */
  public static long zCard(String key) {
    return doCmd("zCard", 0L, t -> t.opsForZSet().zCard(key), key);
  }

  /** 获取大于指定积分的全部元素 */
  public static Set<String> getElementsAboveScore(String key, long score) {
    return doCmd(
        "rangeByScore",
        Collections.emptySet(),
        t -> t.opsForZSet().rangeByScore(key, score, Double.POSITIVE_INFINITY),
        key,
        score);
  }

  /** 移除指定积分范围的元素 */
  public static long removeRangeByScore(String key, long min, long max) {
    return doCmd(
        "removeRangeByScore",
        0L,
        t -> t.opsForZSet().removeRangeByScore(key, min, max),
        key,
        min,
        max);
  }

  /** 获取排名 TopN （rank-排名，从1开始，0-返回全部） */
  public static Set<TypedTuple<String>> getTopN(String key, int rank) {
    return doCmd(
        "getTopN",
        Collections.emptySet(),
        t -> t.opsForZSet().reverseRangeWithScores(key, 0, rank - 1),
        key,
        rank);
  }

  /** 获取排名 TopN （rank-排名，从1开始，0-返回全部） */
  public static Map<Long, ScoreInfo> getTopNMap(String key, int rank) {
    Map<Long, ScoreInfo> map = Maps.newLinkedHashMap();
    int r = 1;
    for (TypedTuple<String> next : getTopN(key, rank)) {
      ScoreInfo info = new ScoreInfo(next, r++);
      map.put(info.getPlayerId(), info);
    }
    return map;
  }

  /**
   * 从小到大移除指定范围的榜单数据
   *
   * @param key key
   * @param start 从0开开始（包含）
   * @param end 包含，-1表示到最后一个元素，-2表示倒数第二个元素
   * @return 移除的数量
   */
  public static Long removeRange(String key, long start, long end) {
    return doCmd("zadd", 0L, t -> t.opsForZSet().removeRange(key, start, end), key, start, end);
  }

  /** 重命名Key */
  public static void rename(String oldKey, String newKey) {
    doCmd(
        "rename",
        null,
        t -> {
          t.rename(oldKey, newKey);
          return null;
        },
        oldKey,
        newKey);
  }
}
