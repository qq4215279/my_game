package com.mumu.framework.util;

import cn.hutool.core.lang.Assert;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
/**
 * SpringContextUtils
 * Spring上下文工具类，用于获取Spring容器中的Bean
 * @author liuzhen
 * @version 1.0.0 2025/3/3 22:16
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
  private static ApplicationContext context;
  private static ApplicationEventPublisher publisher;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    if (context == null) {
      context = applicationContext;
    }
  }

  @Autowired
  public void setPublisher(ApplicationEventPublisher publisher) {
    SpringContextUtils.publisher = publisher;
  }

  /** 取得存储在静态变量中的ApplicationContext. */
  public static ApplicationContext get() {
    checkAndGet();
    return context;
  }

  /** 清除 applicationContext静态变量. */
  public static void clean() {
    context = null;
  }

  private static ApplicationContext checkAndGet() {
    return Assert.notNull(context, "applicationContext 未注入!");
  }

  /** 根据 name 获取 Bean */
  @SuppressWarnings("unchecked")
  public static <T> T getBean(String name) {
    return (T) checkAndGet().getBean(name);
  }

  /** 根据 class 获取 Bean */
  @SuppressWarnings("unchecked")
  public static <T> T getBean(Class<T> clazz) {
    return (T) checkAndGet().getBean(clazz);
  }

  /** 获取标记了指定注解的 bean */
  public static Map<String, Object> getBeansWithAnnotation(
      Class<? extends Annotation> annotationType) {
    return context.getBeansWithAnnotation(annotationType);
  }

  /** 获取容器中全部Bean */
  public static List<Object> getBeans() {
    return Arrays.stream(context.getBeanDefinitionNames()).map(context::getBean).toList();
  }

  /** 根据类型获得 bean集合 */
  public static <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) {
    return context.getBeansOfType(type);
  }


  /** 发布事件 */
  public static void publishEvent(ApplicationEvent event) {
    try {
      publisher.publishEvent(event);
    } catch (Exception e) {
      // LogTopic.ACTION.error(e, "publishEvent", "event", event);
    }
  }
}
