/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil
 * json 工具类
 * @author liuzhen
 * @version 1.0.0 2026/5/2 15:12
 */
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
        if (StringUtils.isEmpty(json) || k == null || v == null) {
            return Collections.emptyMap();
        }
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
        } catch (Exception ex) {
            try {
                return JSON.toJSONString(obj.toString());
            } catch (Exception e) {
                log.error("toJson: {}", obj, e);
            }
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
    public static <T> Map<String, Object> toMap(T t, String... ignoreFields) {
        Map<String, Object> kvMap = fromJson(toJson(t), new TypeToken<Map<String, Object>>() {});
        if (CollUtil.isEmpty(kvMap)) return Collections.emptyMap();
        if (ArrayUtil.isNotEmpty(ignoreFields)) Arrays.stream(ignoreFields).forEach(kvMap::remove);
        return kvMap;
    }

    /**
     * 将json文件转为Map
     * @param path path
     * @return java.util.Map<java.lang.String,java.lang.Integer>
     * @since 2025/6/8 17:27
     */
    public static Map<String, Integer> toMapByJsonFile(String path) {
        Map<String, Integer> map;
        try {
            FileReader reader = new FileReader(path);
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            map = GSON.fromJson(reader, type);

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return map;
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

    public static void main(String[] args) {
        System.out.println(toJson(Collections.emptyList()));
        System.out.println(toJson(Collections.emptyMap()));

        System.out.println(JSON.toJSONString(Collections.emptyList()));
        System.out.println(JSON.toJSONString(Collections.emptyMap()));

        String path = FileUtil.getAbsolutePath(new File("")) + "/GameCore/src/main/java/com/game/framework/core/cmd/consts/cmd.json";
        System.out.println(toMapByJsonFile(path));
    }
}
