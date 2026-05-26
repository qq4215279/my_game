package com.mumu.game.core.drop.anno;

import com.mumu.game.core.drop.consts.DropType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DropMapping
 * 掉落映射， 无引用默认掉落实现为背包
 * @author liuzhen
 * @version 1.0.0 2026/5/24 22:28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DropMapping {

    /**  */
    DropType[] value() default DropType.NONE;
}
