package com.mumu.game.core.autoluban.autoluban2;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import com.cxx.luban.lubanconfigclient.AbLubanConfigLoaderClient;
import com.game.consts.Symbol;
import com.game.framework.core.utils.CovertUtil;
import com.game.framework.core.utils.ModifierUtil;
import com.game.framework.core.utils.RandomUtils;
import com.game.util.JsonUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;

/** 有缓存的通用鲁班 key - value 配置获取接口 @Date: 2024/3/20 17:55 @Author: xu.hai */
public interface AutoLubanParam<T extends AbLubanConfigLoaderClient> extends AutoLubanEvent<T> {
  /** 配置解析后的缓存 k1-类名 k2-枚举名，val-解析后的缓存值 */
  Map<String, Map<String, Object>> CACHE_PARSE = Maps.newHashMapWithExpectedSize(128);

  /** 配置使用中，原始Key生成的代理Key集 k-类名#原始枚举名，val-衍生出的代理key集合 */
  Map<String, Set<String>> CACHE_PROXY_KEYS = Maps.newConcurrentMap();

  /** 注册了配置变更回调的缓存 k-类名#枚举名，val-鲁班配置串 */
  Map<String, String> CACHE_VALUE = Maps.newHashMapWithExpectedSize(128);

  /** 【按需重写】复杂配置自定义解析 注：getProxy()的 key值解析，要用参数传递的 val，不要直接用 toStr() */
  default Object parse(String val) {
    return null;
  }

  /** 【按需重写】注册配置变动回调（当鲁班配置变动后会回调此接口，可用来控制开关变动等，注：此非玩家线程） */
  default BiConsumer<String, String> changeMonitor() {
    return null;
  }

  /** 获取鲁班key */
  String getKey();

  /** 获取默认值 */
  String getDefaultValue();

  /** 根据鲁班key，获取配置值，若为null则返回默认值 */
  String getStringValue(String key, String defaultValue);

  /**
   * 获取新的配置对象 - 用于动态拼接参数key（可按需自定义）<br>
   * 用法： BaseParamEnum.KEY.getProxy(1, 2, 3).getLongValue() 相当于获取鲁班上键为"KEY_1_2_3"的值
   *
   * @param suffixs 动态拼接key（多个用"_"连接）
   * @return 代理配置类
   */
  @SuppressWarnings("untype")
  default AutoLubanParam<T> getProxy(Object... suffixs) {
    AutoLubanParam<T> paramEnum = this;
    return new AutoLubanParam<>() {

      @Override
      public Class<T> getLubanLoaderType() {
        return paramEnum.getLubanLoaderType();
      }

      @Override
      public String getActualClassName() {
        return paramEnum.getActualClassName();
      }

      @Override
      public String getKey() {
        String key = paramEnum.getKey();
        if (ArrayUtil.isEmpty(suffixs)) return key;

        for (Object suffix : suffixs) {
          key += Symbol.UNDERLINE + suffix;
        }
        String cacheKey = getActualClassName() + Symbol.SPLIT_NUMBER + paramEnum.getKey();
        CACHE_PROXY_KEYS.computeIfAbsent(cacheKey, k -> Sets.newConcurrentHashSet()).add(key);
        return key;
      }

      @Override
      public String getDefaultValue() {
        return paramEnum.getDefaultValue();
      }

      @Override
      public String getStringValue(String key, String defaultValue) {
        return paramEnum.getStringValue(key, defaultValue);
      }

      @Override
      public Object parse(String val) {
        return paramEnum.parse(val);
      }
    };
  }

  default String toStr() {
    return getStringValue(getKey(), getDefaultValue());
  }

  default byte getByte() {
    return cache(Byte::parseByte, Byte.class);
  }

  default short getShort() {
    return cache(Short::parseShort, Short.class);
  }

  default int getInt() {
    return cache(Integer::parseInt, Integer.class);
  }

  default long getLong() {
    return cache(Long::parseLong, Long.class);
  }

  default float getFloat() {
    return cache(Float::parseFloat, Float.class);
  }

  default double getDouble() {
    return cache(Double::parseDouble, Double.class);
  }

  default BigDecimal getBigDecimal() {
    return cache(BigDecimal::new, BigDecimal.class);
  }

  /** 匹配的字符串为true ("true", "yes", "y", "t", "ok", "1", "on", "是", "对", "真", "對", "√") */
  default boolean getBool() {
    return cache(BooleanUtil::toBoolean, Boolean.class);
  }

  /** 获取日期毫秒时间戳 */
  default long getMillis() {
    return get(Long.class);
  }

  default int[] getIntArr() {
    return get(int[].class);
  }

  default float[] getFloatArr() {
    return get(float[].class);
  }

  default long[] getLongArr() {
    return get(long[].class);
  }

  default double[] getDoubleArr() {
    return get(double[].class);
  }

  default <T> List<T> getList() {
    return JsonUtil.getType(get(List.class));
  }

  default <T> Set<T> getSet() {
    return JsonUtil.getType(get(Set.class));
  }

  default <K, V> Map<K, V> getMap() {
    return JsonUtil.getType(get(Map.class));
  }

  default <K, V> TreeMap<K, V> getTreeMap() {
    return JsonUtil.getType(get(TreeMap.class));
  }

  default <K> WeightRandom<K> getWeightRandom() {
    return JsonUtil.getType(get(WeightRandom.class));
  }

  /** 获取指定类型的配置（注：调用此方法需实现parse自定义解析） */
  default <T> T get(Class<T> clazz) {
    return cache(val -> JsonUtil.getType(parse(val)), clazz);
  }

  /**
   * 获取缓存值，若缓存未命中，则解析配置并缓存
   *
   * @param parseFun 配置解析函数
   * @param clazz 解析后的类型
   * @return 解析后的对象
   * @param <T> 解析后的类型
   */
  default <T> T cache(Function<String, T> parseFun, Class<T> clazz) {
    Map<String, Object> cache =
        CACHE_PARSE.computeIfAbsent(getActualClassName(), k -> Maps.newConcurrentMap());
    String key = getKey();
    Object res = cache.get(key);
    // 缓存存在 && 缓存值类型匹配，直接返回缓存
    if (res != null && clazz.isAssignableFrom(res.getClass())) {
      return JsonUtil.getType(res);
    }
    // 解析配置并缓存
    String val = toStr();
    T parse = parseFun.apply(val);
    Assert.notNull(parse, "KV配置解析失败：" + getActualClassName() + "#" + key + " -> " + val);
    cache.put(key, parse);
    return parse;
  }

  /** 获取真实的类全限定名（代理对象，会截取后面的掉$1） */
  default String getActualClassName() {
    return StringUtils.substringBefore(this.getClass().getName(), Symbol.SPLIT_DOLLAR);
  }

  /** 鲁班回调刷新 */
  default void autoLubanRefresh() {
    // 刷新原始key
    refresh(getKey());

    // 刷新代理key
    String classEnumKey = getActualClassName() + Symbol.SPLIT_NUMBER + getKey();
    CACHE_PROXY_KEYS.getOrDefault(classEnumKey, Collections.emptySet()).forEach(this::refresh);
  }

  /** 刷新指定key的缓存 */
  private void refresh(String key) {
    String className = getActualClassName();
    Map<String, Object> cache =
        CACHE_PARSE.computeIfAbsent(className, k -> Maps.newConcurrentMap());

    String newVal = getStringValue(key, getDefaultValue());
    // 移除老的key缓存
    cache.remove(key);
    // 尝试解析最新的鲁班配置，并更新到指定缓存中
    Optional.ofNullable(parse(newVal)).ifPresent(val -> cache.put(key, val));
    // 若注册了缓存变动回调，则检查配置是否变动，若变动则执行回调
    BiConsumer<String, String> monitor = changeMonitor();
    if (monitor == null) return;
    String classEnumKey = className + Symbol.SPLIT_NUMBER + key;
    String oldVal = CACHE_VALUE.put(classEnumKey, newVal);
    if (oldVal != null && !oldVal.equals(newVal)) monitor.accept(key, oldVal);
  }

  @Override
  default int order() {
    return -10;
  }

  /** 判断指定class是否不能加载 true-不能加载 */
  static boolean cannotLoad(Class<?> clazz) {
    return clazz == null
        || !AutoLubanParam.class.isAssignableFrom(clazz)
        || !Enum.class.isAssignableFrom(clazz)
        // Enum的匿名内部类获取到的SimpleName为空
        || ModifierUtil.isProxyClass(clazz);
  }

  // ----------------------- 工具转换方法 -----------------------

  // default long toTime(String pattern) {
  //   return DateUtil.parseMillis(toStr(), pattern);
  // }
  //
  // default <T> T toJson(Class<T> clazz) {
  //   return JsonUtil.fromJson(toStr(), clazz);
  // }
  //
  // default int[] toIntArr(String split) {
  //   return CovertUtil.stringToIntArr(toStr(), split);
  // }
  //
  // default long[] toLongArr(String split) {
  //   return CovertColletionUtil.stringToLongArr(toStr(), split);
  // }
  //
  // default double[] toDoubleArr(String split) {
  //   return CovertColletionUtil.stringToDoubleArr(toStr(), split);
  // }

  default <T> List<T> toList(String split, Class<T> type) {
    return CovertUtil.stringToList(toStr(), split, type);
  }

  default <T> Set<T> toSet(String split, Class<T> type) {
    return CovertUtil.stringToSet(toStr(), split, type);
  }

  default <K, V> Map<K, V> toMap(String subSplit, String split, Class<K> kType, Class<V> vType) {
    return CovertUtil.stringToMap(toStr(), subSplit, split, kType, vType);
  }

  /** 命中概率 */
  default boolean hitInt() {
    return RandomUtils.hit(getInt());
  }

  /** 命中概率 */
  default boolean hitInt10000() {
    return RandomUtils.hit10000(getInt());
  }

  /** 命中概率(指定概率和精度) */
  default boolean hitIntArr() {
    int[] range = getIntArr();
    if (range.length != 2) return false;
    return RandomUtils.hit(range[0], range[1]);
  }

  /** 从[min, max)中随机一个int值 */
  default int randomInt() {
    int[] range = getIntArr();
    if (range.length != 2) return 0;
    return RandomUtils.randomInt(range[0], range[1]);
  }

  /** 从[min, max]中随机一个int值（注，与randomInt的[min, max)随机范围不同） */
  default int rollInt() {
    return RandomUtils.rollInt(getIntArr());
  }

  /** 从list中随机出一个元素 */
  default <T> T randomList() {
    return RandomUtil.randomEle(getList());
  }

  /** 从list中随机出N个元素（不可重复） */
  default <T> List<T> randomEleList(int num) {
    return RandomUtil.randomEleList(getList(), num);
  }

  /** 从list中随机出N个元素（可重复） */
  default <T> List<T> randomEles(int num) {
    return RandomUtil.randomEles(getList(), num);
  }

  // /** 从权重Map(key-随机的元素，val-权重)中随出一个元素 */
  // default <K> K randomMap() {
  //   return RandomUtil.randomMap(getMap());
  // }
  //
  // /** 【不重复】从权重Map(key-随机的元素，val-权重)中随出N个元素 */
  // default <T> Set<T> randomMap(int num) {
  //   return RandomUtil.randomMap(getMap(), num);
  // }
  //
  // /** 【可能重复】从权重Map(key-随机的元素，val-权重)中随出N个元素 */
  // default <T> List<T> randomRepeatableMap(int num) {
  //   return RandomUtil.randomRepeatableMap(getMap(), num);
  // }
}
