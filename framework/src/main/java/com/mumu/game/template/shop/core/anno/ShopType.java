package com.mumu.game.template.shop.core.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * ShopType
 * 商品类型注解
 * @author liuzhen
 * @version 1.0.0 2024/11/19 20:39
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
@Scope("prototype")
public @interface ShopType {

  /**
   * 商品类型
   * @return int
   * @since 2024/11/19 20:39
   */
  int[] value() default 0;
}
