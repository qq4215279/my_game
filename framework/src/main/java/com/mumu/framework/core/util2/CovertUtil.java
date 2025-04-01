/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mumu.framework.core.log.LogTopic;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;

/** 转换工具类 @Date: 2024/8/5 下午2:11 @Author: xu.hai */
@SuppressWarnings("unchecked")
public class CovertUtil {
  private static final Map<Class<?>, Function<String, ?>> parseTpyeFunMap = new HashMap<>();

  /** 大素数 */
  static final long CMD_PRIME = 982451653L;

  static {
    parseTpyeFunMap.put(Byte.class, Byte::parseByte);
    parseTpyeFunMap.put(Short.class, Short::parseShort);
    parseTpyeFunMap.put(Integer.class, Integer::parseInt);
    parseTpyeFunMap.put(Long.class, Long::parseLong);
    parseTpyeFunMap.put(Float.class, Float::parseFloat);
    parseTpyeFunMap.put(String.class, x -> x);
  }

  /** 将hashCode映射到[100000, 999999]范围内 */
  public static int convertToCmdId(int hashCode) {
    // 使用绝对值，确保正数
    long positiveHashCode = Math.abs((long) hashCode);
    // 使用一个大素数进行混合,将值缩放到 0 到 899,999 之间
    long scaledValue = (positiveHashCode * CMD_PRIME) % 900000;
    // 加上偏移量，确保范围在 [100000, 999999]
    return (int) scaledValue + 100000;
  }

  /** 将两个ID（如玩家ID），映射到唯一ID上（可用于生成玩家聊天ID） */
  public static long covertToUnionId(long a, long b) {
    long min = Math.min(a, b), max = Math.max(a, b);
    long hashA = min ^ (min >>> 32);
    long hashB = max ^ (max >>> 32);
    return (hashA << 32) | (hashB & 0xFFFFFFFFL);
  }

  /** 有一个为blank则为true */
  private static boolean isBlank(String... params) {
    if (params == null) return false;
    for (String param : params) {
      if (StringUtils.isBlank(param)) {
        return true;
      }
    }
    return false;
  }

  /** string To int[] */
  public static int[] stringToIntArr(String value, String split) {
    if (isBlank(value, split)) return new int[0];
    StringTokenizer tokenizer = new StringTokenizer(value, split);
    int[] arr = new int[tokenizer.countTokens()];
    int i = 0;
    while (tokenizer.hasMoreTokens()) {
      arr[i++] = Integer.parseInt(tokenizer.nextToken());
    }
    return arr;
  }

  public static Set<String> stringToSet(String value, String split) {
    return stringToCollection(value, split, String.class, Sets::newHashSet);
  }

  public static <T> Set<T> stringToSet(String value, String split, Class<T> type) {
    return stringToCollection(value, split, type, Sets::newHashSet);
  }

  public static List<String> stringToList(String value, String split) {
    return stringToCollection(value, split, String.class, Lists::newArrayList);
  }

  public static List<Integer> stringToIntList(String value, String split) {
    return stringToList(value, split, Integer.class);
  }

  public static <T> List<T> stringToList(String value, String split, Class<T> type) {
    return stringToCollection(value, split, type, Lists::newArrayList);
  }

  public static <T, R extends Collection<T>> R stringToCollection(
      String value, String split, Class<T> type, Supplier<R> supplier) {
    return stringToCollection(value, split, parseTpyeFunMap.get(type), supplier);
  }

  /** String -> Collection 不会返回null（注：supplier不能传null） */
  public static <T, R extends Collection<T>> R stringToCollection(
      String value, String split, Function<String, ?> func, Supplier<R> supplier) {
    Objects.requireNonNull(supplier, "supplier is null");
    if (isBlank(value, split) || func == null) {
      return supplier.get();
    }
    try {
      R collection = supplier.get();
      StringTokenizer tokenizer = new StringTokenizer(value, split);
      while (tokenizer.hasMoreTokens()) {
        Object apply = func.apply(tokenizer.nextToken());
        if (apply != null) collection.add((T) apply);
      }
      return collection;
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "stringToList", "value", value, "split", split);
      return supplier.get();
    }
  }

  public static <K, V> String pairToString(Pair<K, V> pair, String split) {
    return pairToString(pair, split, Object::toString, Object::toString);
  }

  public static <K, V> String pairToString(
      Pair<K, V> pair, String split, Function<K, String> keyFunc, Function<V, String> valFunc) {
    if (pair == null) return StringUtils.EMPTY;
    try {
      return keyFunc.apply(pair.getKey()) + split + valFunc.apply(pair.getValue());
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "pairToString", "pair", pair, "split", split);
      return StringUtils.EMPTY;
    }
  }

  public static <K, V> List<Pair<K, V>> stringToPairList(
      String value, String subSplit, String split, Class<K> keyType, Class<V> valueType) {
    return stringToCollection(
        value, split, v -> stringToPair(v, subSplit, keyType, valueType), Lists::newArrayList);
  }

  public static <K, V> Pair<K, V> stringToPair(
      String value, String split, Class<K> keyType, Class<V> valueType) {
    return stringToPair(value, split, parseTpyeFunMap.get(keyType), parseTpyeFunMap.get(valueType));
  }

  public static <K, V> Pair<K, V> stringToPair(
      String value, String split, Function<String, ?> funcKey, Function<String, ?> funcValue) {
    if (isBlank(value, split) || funcKey == null || funcValue == null) {
      return null;
    }
    try {
      String[] arr = value.split(split);
      Assert.isTrue(arr.length == 2, "stringToPair length is not 2");
      return Pair.of((K) funcKey.apply(arr[0]), (V) funcValue.apply(arr[1]));
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "stringToPair", "value", value, "split", split);
      return null;
    }
  }

  public static Map<Integer, Integer> stringToIntMap(String value, String subSplit, String split) {
    return stringToMap(value, subSplit, split, Integer.class, Integer.class);
  }

  public static <K, V> Map<K, V> stringToMap(
      String value, String subSplit, String split, Class<K> kType, Class<V> vType) {
    return stringToMap(value, subSplit, split, kType, vType, null);
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> stringToMap(
      String value,
      String subSplit,
      String split,
      Class<K> kType,
      Class<V> vType,
      Supplier<Map<K, V>> mapSupplier) {
    return stringToMap(
        value,
        subSplit,
        split,
        (Function<String, K>) parseTpyeFunMap.get(kType),
        (Function<String, V>) parseTpyeFunMap.get(vType),
        mapSupplier);
  }

  public static <K, V> Map<K, V> stringToMap(
      String value,
      String subSplit,
      String split,
      Function<String, K> kFun,
      Function<String, V> vFun) {
    return stringToMap(value, subSplit, split, kFun, vFun, null);
  }

  /**
   * String -> Map 不会返回null
   *
   * @param value 目标串
   * @param subSplit 二级分隔
   * @param split 一级分隔
   * @param kFun key转换函数
   * @param vFun value转换函数
   * @param mapSupplier 获取Map对象（为空时自动转换成HashMap）
   * @return Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> stringToMap(
      String value,
      String subSplit,
      String split,
      Function<String, K> kFun,
      Function<String, V> vFun,
      Supplier<Map<K, V>> mapSupplier) {
    if (mapSupplier == null) {
      mapSupplier = HashMap::new;
    }
    Map<K, V> map = mapSupplier.get();
    if (isBlank(value, subSplit, split) || kFun == null || vFun == null) {
      return map;
    }
    try {
      StringTokenizer tokenizer = new StringTokenizer(value, split);
      while (tokenizer.hasMoreTokens()) {
        StringTokenizer subTokenizer = new StringTokenizer(tokenizer.nextToken(), subSplit);
        map.put(kFun.apply(subTokenizer.nextToken()), vFun.apply(subTokenizer.nextToken()));
      }
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "stringToMap", "value", value, "subSplit", subSplit, "split", split);
    }
    return map;
  }

  /** collection --> String 不会反回null */
  public static <T> String collToString(Collection<T> collection, String split) {
    return collToString(collection, split, Object::toString);
  }

  /** collection --> String 不会反回null */
  public static <T> String collToString(
      Collection<T> collection, String split, Function<T, String> func) {
    if (CollectionUtil.isEmpty(collection)) {
      return StringUtils.EMPTY;
    }
    return collection.stream().map(func).collect(Collectors.joining(split));
  }

  /** map --> String 不会反回null */
  public static String mapToString(Map<?, ?> map, String subSplit, String split) {
    return mapToString(map, subSplit, split, Object::toString, Object::toString);
  }

  /**
   * map --> String
   *
   * @param map 待转换Map
   * @param subSplit 连接单个 kv
   * @param split 连接多个kv值
   * @param kFun key 转换String
   * @param vFun value 转换String
   * @return 不会反回 null
   */
  public static <K, V> String mapToString(
      Map<K, V> map,
      String subSplit,
      String split,
      Function<K, String> kFun,
      Function<V, String> vFun) {
    if (MapUtil.isEmpty(map) || isBlank(subSplit, split) || kFun == null || vFun == null) {
      return StringUtils.EMPTY;
    }
    StringBuilder sb = new StringBuilder();
    map.forEach(
        (k, v) -> sb.append(kFun.apply(k)).append(subSplit).append(vFun.apply(v)).append(split));
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  /** 拆分大 Map为多个小 Map */
  public static <K, V> List<Map<K, V>> partition(Map<K, V> map, int size) {
    Assert.isTrue(size > 0, "partition size > 0, {}", size);
    if (CollUtil.isEmpty(map)) return Collections.emptyList();
    return Lists.partition(Lists.newArrayList(map.entrySet()), size).stream()
        .map(eachList -> CollStreamUtil.toMap(eachList, Entry::getKey, Entry::getValue))
        .toList();
  }
}
