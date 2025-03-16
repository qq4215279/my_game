package com.mumu.common.util2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/** Json 工具类（使用Gson工具） @Date: 2024/8/5 下午2:29 @Author: xu.hai */
@Slf4j
public class JsonUtil {
  private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

  /** json 转 list */
  public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json) || clazz == null) return Collections.emptyList();
    try {
      return GSON.fromJson(json, TypeToken.getParameterized(List.class, clazz).getType());
    } catch (Exception e) {
      log.error("json: {}, clazz: {}", json, clazz, e);
    }
    return Collections.emptyList();
  }

  /** json 转 map */
  public static <K, V> Map<K, V> fromJsonMap(String json, Class<K> k, Class<V> v) {
    if (StringUtils.isEmpty(json) || k == null || v == null) return Collections.emptyMap();
    try {
      return GSON.fromJson(json, TypeToken.getParameterized(Map.class, k, v).getType());
    } catch (Exception e) {
      log.error("json: {}, k: {}, v: {}", json, k, v, e);
    }
    return Collections.emptyMap();
  }

  /** json 转指定 class 类型 */
  public static <T> T fromJson(String json, Class<T> classOfT) {
    if (StringUtils.isEmpty(json) || classOfT == null) return null;
    try {
      return GSON.fromJson(json, classOfT);
    } catch (Exception e) {
      log.error("json: {}, class: {}", json, classOfT, e);
    }
    return null;
  }

  /** json 转指定 typeToken 类型 */
  public static <T> T fromJson(String json, TypeToken<T> typeToken) {
    if (StringUtils.isEmpty(json) || typeToken == null) return null;
    try {
      return GSON.fromJson(json, typeToken.getType());
    } catch (Exception e) {
      log.error("json: {}, typeToken: {}", json, typeToken, e);
    }
    return null;
  }

  /** 对象转 json */
  public static String toJson(Object obj) {
    if (obj == null) return StringUtils.EMPTY;

    if (obj instanceof String str) return str;

    try {
      return GSON.toJson(obj);
    } catch (Exception e) {
      log.error("json: {}", obj, e);
    }
    return StringUtils.EMPTY;
  }

  /**
   * 将Bean对象转为Map，key-字段名，value-字段值
   *
   * @param t 任意对象类型
   * @param ignoreFields 忽略的字段集
   * @param <T> 任意对象类型
   * @return 返回Map
   */
  public static <T> Map<String, String> toMap(T t, String... ignoreFields) {
    Map<String, String> kvMap = fromJson(toJson(t), new TypeToken<Map<String, String>>() {});
    if (CollUtil.isEmpty(kvMap)) return Collections.emptyMap();
    if (ArrayUtil.isNotEmpty(ignoreFields)) Arrays.stream(ignoreFields).forEach(kvMap::remove);
    return kvMap;
  }

  /** 对象转换为指定类型（需确定是指定类型才能使用，可能为null） */
  @SuppressWarnings("unchecked")
  public static <T> T getType(Object object) {
    return (T) object;
  }

  // private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  // static {
  //   OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  //   OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  //   OBJECT_MAPPER.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
  // }
  //
  // /** Object转Json格式数据 */
  // public static String toJson(Object object) {
  //   MappingJsonFactory f = new MappingJsonFactory();
  //   StringWriter sw = new StringWriter();
  //   try {
  //     JsonGenerator generator = f.createGenerator(sw);
  //     generator.writeObject(object);
  //     generator.close();
  //   } catch (Exception e) {
  //     log.error("JsonUtil.toJson error! object = {}", object, e);
  //   }
  //   return sw.toString();
  // }
  //
  // public static <T> T toObj(String json, Class<T> cls) {
  //   try {
  //     if (StringUtils.isNotBlank(json) && cls != null) {
  //       return OBJECT_MAPPER.readValue(json, cls);
  //     }
  //   } catch (Exception e) {
  //     log.error("JsonUtil.toObj error! json = {}, cls = {}", json, cls, e);
  //   }
  //   return null;
  // }

}
