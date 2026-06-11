package com.mumu.game.core.task.anno;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TaskType
 * 任务类型注解
 * @author liuzhen
 * @version 1.0.0 2026/6/11 18:02
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Component
@Scope("prototype")
public @interface TaskType {

    /**
     * 任务类型
     * @return int[]
     * @date 2026/6/11 18:03
     */
    int[] value() default 0;
}
