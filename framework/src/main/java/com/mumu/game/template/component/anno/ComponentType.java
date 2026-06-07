package com.mumu.game.template.component.anno;

import com.mumu.game.template.component.enums.ComponentTypeEnum;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ComponentType
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:19
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Component
public @interface ComponentType {

    /**
     * 组件id
     * @return com.game.template.activity.enums.ActivityTypeEnum
     * @since 2025/3/15 21:04
     */
    ComponentTypeEnum value();
}
