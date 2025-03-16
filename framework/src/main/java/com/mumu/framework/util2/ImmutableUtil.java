package com.mumu.framework.util2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ImmutableUtil
 * 不可变集合工具类
 * @author liuzhen
 * @version 1.0.0 2024/10/21 11:09
 */
public class ImmutableUtil {

  /**
   * 将list，根据Key，转换成 ImmutableMap<K, V> 复合数据结构
   * @param list list
   * @param convert 提供Key
   * @return com.google.common.collect.ImmutableMap<K,V>
   * @date 2024/10/29 18:09
   */
  public static <K, V> ImmutableMap<K, V> list2ImmMap(Collection<V> list, Function<V, K> convert) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Builder<K, V> builder = ImmutableMap.builder();
    for (V v : list) {
      K k = convert.apply(v);
      if (k == null) {
        continue;
      }

      builder.put(k, v);
    }
    return builder.build();
  }

  /**
   * 将list，转成多个key映射一个V的 ImmutableMap<K, V> 复合数据结构
   * @param list list
   * @param convert 提供一个list集合key
   * @return com.google.common.collect.ImmutableMap<K,V>
   * @date 2024/10/29 18:22
   */
  public static <K, V> ImmutableMap<K, V> list2MultiKeyImmMap(Collection<V> list, Function<V, List<K>> convert) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Builder<K, V> builder = ImmutableMap.builder();
    for (V v : list) {
      List<K> k = convert.apply(v);
      if (k == null) {
        continue;
      }

      for (K k1 : k) {
        builder.put(k1, v);
      }
    }
    return builder.build();
  }

  /**
   * 将List，转成 ImmutableMap<K, ImmutableList<V>> 复合数据结构
   * @param list list
   * @param convert 提供一个key
   * @return com.google.common.collect.ImmutableMap<K,com.google.common.collect.ImmutableList<V>>
   * @date 2024/10/29 18:24
   */
  public static <K, V> ImmutableMap<K, ImmutableList<V>> list2ImmMapWithList(Collection<V> list, Function<V, K> convert) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Map<K, ImmutableList.Builder<V>> tmpMap = Maps.newHashMap();
    for (V v : list) {
      K mainKey = convert.apply(v);
      if (mainKey == null) {
        continue;
      }

      ImmutableList.Builder<V> listBuilder = tmpMap.computeIfAbsent(mainKey,
          o -> ImmutableList.builder());
      listBuilder.add(v);
    }

    Builder<K, ImmutableList<V>> builder = ImmutableMap.builder();
    for (Map.Entry<K, ImmutableList.Builder<V>> entry : tmpMap.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().build());
    }

    return builder.build();
  }

  /**
   * 将list，根据key分组，转成 ImmutableMap<K, ImmutableList<V>> 复合数据结构
   * @param list list
   * @param convert 提供一个key
   * @return com.google.common.collect.ImmutableMap<K,com.google.common.collect.ImmutableList<V>>
   * @date 2024/10/29 18:28
   */
  public static <K, V> ImmutableMap<K, ImmutableList<V>> list2ImmMapWithMultiKeyList(Collection<V> list, Function<V, List<K>> convert) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Map<K, ImmutableList.Builder<V>> tmpMap = Maps.newHashMap();
    for (V v : list) {
      List<K> mainKeyList = convert.apply(v);
      for (K mainKey : mainKeyList) {
        if (mainKey == null) {
          continue;
        }

        ImmutableList.Builder<V> listBuilder = tmpMap.computeIfAbsent(mainKey,
            o -> ImmutableList.builder());
        listBuilder.add(v);
      }
    }

    Builder<K, ImmutableList<V>> builder = ImmutableMap.builder();
    for (Map.Entry<K, ImmutableList.Builder<V>> entry : tmpMap.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().build());
    }

    return builder.build();
  }

  /**
   * 将List，转成 ImmutableMap<K1, ImmutableMap<K2, V>> 复合数据结构
   * @param list list
   * @param convert 提供key1
   * @param convert2 提供key2
   * @return com.google.common.collect.ImmutableMap<K,com.google.common.collect.ImmutableMap<X,V>>
   * @date 2024/10/29 20:02
   */
  public static <K1, K2, V> ImmutableMap<K1, ImmutableMap<K2, V>> list2ImmMapWithSubMap(Collection<V> list,
      Function<V, K1> convert, Function<V, K2> convert2) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Map<K1, Builder<K2, V>> tmpMap = Maps.newHashMap();
    for (V v : list) {
      K1 mainKey = convert.apply(v);
      if (mainKey == null) {
        continue;
      }

      Builder<K2, V> k2VBuilder = tmpMap.computeIfAbsent(mainKey, k -> ImmutableMap.builder());
      K2 subKey = convert2.apply(v);
      k2VBuilder.put(subKey, v);
    }

    Builder<K1, ImmutableMap<K2, V>> builder = ImmutableMap.builder();
    for (Map.Entry<K1, Builder<K2, V>> entry : tmpMap.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().build());
    }

    return builder.build();
  }

  /**
   * 将list，转成 ImmutableMap<K1, ImmutableMap<K2, ImmutableMap<K3, V>>> 复合数据结构
   * @param list list
   * @param convert 提供Key1
   * @param convert2 提供Key2
   * @param convert3 提供Key3
   * @return com.google.common.collect.ImmutableMap<K,java.util.Map<X,java.util.Map<Y,V>>>
   * @date 2024/10/29 20:07
   */
  public static <K1, V, K2, K3> ImmutableMap<K1, ImmutableMap<K2, ImmutableMap<K3, V>>> list2ImmMapWithSubSubMap(Collection<V> list,
      Function<V, K1> convert, Function<V, K2> convert2, Function<V, K3> convert3) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Map<K1, Map<K2, Map<K3, V>>> tmpMap = Maps.newHashMap();
    for (V v : list) {
      K1 mainKey = convert.apply(v);
      Map<K2, Map<K3, V>> k2MapMap = tmpMap.computeIfAbsent(mainKey, k -> Maps.newHashMap());

      K2 k2Key = convert2.apply(v);
      Map<K3, V> k3VMap = k2MapMap.computeIfAbsent(k2Key, k -> Maps.newHashMap());

      K3 k3Key = convert3.apply(v);
      k3VMap.put(k3Key, v);
    }
    // return ImmutableMap.copyOf(tmpMap);

    Builder<K1, ImmutableMap<K2, ImmutableMap<K3, V>>> builder = ImmutableMap.builder();
    for (Map.Entry<K1, Map<K2, Map<K3, V>>> entry : tmpMap.entrySet()) {
      K1 k1 = entry.getKey();

      Builder<K2, ImmutableMap<K3, V>> k2Builder = ImmutableMap.builder();
      for (Map.Entry<K2, Map<K3, V>> entry1 : entry.getValue().entrySet()) {
        K2 k2 = entry1.getKey();

        Builder<K3, V> k3Builder = ImmutableMap.builder();
        for (Map.Entry<K3, V> entry2 : entry1.getValue().entrySet()) {
          K3 k3 = entry2.getKey();
          V v = entry2.getValue();
          k3Builder.put(k3, v);
        }

        k2Builder.put(k2, k3Builder.build());
      }

      builder.put(k1, k2Builder.build());
    }

    return builder.build();
  }

  /**
   * 将list，转成 ImmutableMap<K1, ImmutableMap<K2, ImmutableList<V>>> 复合数据结构
   * @param list list
   * @param convert 提供Key1
   * @param convert2 提供Key2
   * @return com.google.common.collect.ImmutableMap<K,java.util.Map<X,java.util.List<V>>>
   * @date 2024/10/29 20:19
   */
  public static <K1, K2, V> ImmutableMap<K1, ImmutableMap<K2, ImmutableList<V>>> list2ImmMapWithSubMapListMap(Collection<V> list,
      Function<V, K1> convert, Function<V, K2> convert2) {
    if (list == null || convert == null) {
      return ImmutableMap.of();
    }

    Map<K1, Map<K2, List<V>>> tmpMap = Maps.newHashMap();
    for (V v : list) {
      K1 mainKey = convert.apply(v);
      Map<K2, List<V>> k2ListMap = tmpMap.computeIfAbsent(mainKey, k -> Maps.newHashMap());

      K2 k2Key = convert2.apply(v);
      List<V> subList = k2ListMap.computeIfAbsent(k2Key, k -> Lists.newArrayList());
      subList.add(v);
    }
    // return ImmutableMap.copyOf(tmpMap);

    Builder<K1, ImmutableMap<K2, ImmutableList<V>>> builder = ImmutableMap.builder();
    for (Map.Entry<K1, Map<K2, List<V>>> entry : tmpMap.entrySet()) {
      K1 k1 = entry.getKey();

      Builder<K2, ImmutableList<V>> k2builder = ImmutableMap.builder();
      for (Map.Entry<K2, List<V>> entry1 : entry.getValue().entrySet()) {
        K2 k2 = entry1.getKey();

        ImmutableList.Builder<V> listBuilder = ImmutableList.builder();
        for (V v : entry1.getValue()) {
          listBuilder.add(v);
        }

        k2builder.put(k2, listBuilder.build());
      }

      builder.put(k1, k2builder.build());
    }

    return builder.build();
  }

  public static void main(String[] args) {
    // test
    List<ImmutableOb> list = new ArrayList<>();
    list.add(new ImmutableOb(1, 1, 1, 1, "value1", Lists.newArrayList(1, 2, 3)));
    list.add(new ImmutableOb(2, 2, 2, 1, "value2", Lists.newArrayList(4, 5, 6)));
    list.add(new ImmutableOb(3, 3, 3, 2, "value3", Lists.newArrayList(7, 8, 9)));
    list.add(new ImmutableOb(4, 4, 4, 2, "value4", Lists.newArrayList(10, 11, 12)));
    list.add(new ImmutableOb(5, 5, 5, 3, "value5", Lists.newArrayList(13, 14, 15)));

    // 1. list2ImmMap
    ImmutableMap<Integer, ImmutableOb> key1ObMap = ImmutableUtil.list2ImmMap(
        list, ImmutableOb::getKey1);
    System.out.println("1. list2ImmMap() -------- key1ObMap: " + key1ObMap);

    // 2. list2MultiKeyImmMap
    ImmutableMap<Integer, ImmutableOb> keysObMap = ImmutableUtil.list2MultiKeyImmMap(
        list, ImmutableOb::getKeys);
    System.out.println("2. list2MultiKeyImmMap -------- keysObMap: " + keysObMap);

    // 3. list2ImmMapWithList
    ImmutableMap<Integer, ImmutableList<ImmutableOb>> typeObsMap = ImmutableUtil.list2ImmMapWithList(
        list, ImmutableOb::getType);
    System.out.println("3. list2ImmMapWithList -------- typeObsMap: " + typeObsMap);

    // 4. list2ImmMapWithMultiKeyList
    ImmutableMap<Integer, ImmutableList<ImmutableOb>> multiKeysObsMap = ImmutableUtil.list2ImmMapWithMultiKeyList(
        list, ImmutableOb::getKeys);
    System.out.println("4. list2ImmMapWithMultiKeyList -------- multiKeysObsMap: " + multiKeysObsMap);

    // 5. list2ImmMapWithSubMap
    ImmutableMap<Integer, ImmutableMap<Integer, ImmutableOb>> key1Key2ObMap = ImmutableUtil.list2ImmMapWithSubMap(
        list, ImmutableOb::getKey1, ImmutableOb::getKey2);
    System.out.println("5. list2ImmMapWithSubMap -------- key1Key2ObMap: " + key1Key2ObMap);

    // 6. list2ImmMapWithSubSubMap
    ImmutableMap<Integer, ImmutableMap<Integer, ImmutableMap<Integer, ImmutableOb>>> key1Key2Key3ObMap = ImmutableUtil.list2ImmMapWithSubSubMap(
        list, ImmutableOb::getKey1, ImmutableOb::getKey2, ImmutableOb::getKey3);
    System.out.println("6. list2ImmMapWithSubSubMap -------- key1Key2Key3ObMap: " + key1Key2Key3ObMap);

    // 7. list2ImmMapWithSubMapListMap
    ImmutableMap<Integer, ImmutableMap<Integer, ImmutableList<ImmutableOb>>> typeKey1ListMap = ImmutableUtil.list2ImmMapWithSubMapListMap(
        list, ImmutableOb::getType, ImmutableOb::getKey1);
    System.out.println("7. list2ImmMapWithSubMapListMap -------- typeKey1ListMap: " + typeKey1ListMap);

  }

  @Data
  @AllArgsConstructor
  private static final class ImmutableOb {
    int key1;
    int key2;
    int key3;
    int type;
    String value;
    List<Integer> keys;
  }

}
