package com.mumu.game.gm.anno;

import com.mumu.game.gm.enums.GmCommand;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GmConfig
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/5 20:15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmConfig {

    /** 命令 */
    GmCommand gmCmd();
}
