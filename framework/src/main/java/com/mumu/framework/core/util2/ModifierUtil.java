/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import autovalue.shaded.com.google.common.common.collect.Maps;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/** 修饰符工具类 @Date: 2024/3/13 14:37 @Author: xu.hai */
public class ModifierUtil extends cn.hutool.core.util.ModifierUtil {

  /** 是否是【公开静态】方法 */
  public static boolean isPublicStatic(Method method) {
    return isPublic(method) && isStatic(method);
  }

  /** 是否是【公开静态无参】方法 */
  public static boolean isPublicStaticEmpty(Method method) {
    return isPublicStatic(method) && ArrayUtil.isEmpty(method.getParameterTypes());
  }

  public static boolean isBelongTo(Class<?> var, Class<?> origin) {
    return origin.isAssignableFrom(var);
  }

  public static boolean isInt(Class<?> param) {
    return isBelongTo(Integer.class, param) || isBelongTo(int.class, param);
  }

  public static boolean isBool(Class<?> param) {
    return isBelongTo(Boolean.class, param) || isBelongTo(boolean.class, param);
  }

  /** 判断是否为代理类型 */
  public static boolean isProxyClass(Class<?> clazz) {
    return StringUtils.isBlank(clazz.getSimpleName());
  }

  /** 查找实现类（clazz）上指定接口（rawType）中指定类型（superClass）的泛型声名 */
  public static <T extends S, S> Class<T> getGenericInterfaceClass(
      Class<?> clazz, Class<?> rawType, Class<S> superClass) {
    for (Type genType : clazz.getGenericInterfaces()) {
      Class<T> res = getGenericClass(genType, rawType, superClass);
      if (res != null) return res;
    }
    return null;
  }

  /** 查找实现类上父类中指定类型的泛型声名 */
  public static <T extends S, S> Class<T> getGenericSuperClass(
      Class<?> clazz, Class<S> superClass) {
    return getGenericClass(clazz.getGenericSuperclass(), null, superClass);
  }

  /** 查找指定类上指定指定类型的泛型声名 */
  public static <T extends S, S> Class<T> getGenericClass(
      Type genType, Class<?> rawType, Class<S> superClass) {
    if (genType instanceof ParameterizedType paramType) {
      if (rawType == null || rawType.isAssignableFrom((Class<?>) paramType.getRawType())) {
        for (Type type : paramType.getActualTypeArguments()) {
          if (type instanceof Class client
              && (superClass == null || superClass.isAssignableFrom(client))) {
            return client;
          }
        }
      }
    }
    return null;
  }

  /** 检查类中是否存在定义了多个相同值的属性 */
  public static void checkDuplicateField(Class<?> clazz) {
    Map<Object, String> map = Maps.newHashMap();
    for (Field f : clazz.getDeclaredFields()) {
      String name = f.getName();
      Object value = ReflectUtil.getStaticFieldValue(f);
      Assert.isFalse(
          map.containsKey(value), "发现重复的属性值[{}]！k1: {}, K2: {}", value, name, map.get(value));
      map.put(value, name);
    }
  }
}
