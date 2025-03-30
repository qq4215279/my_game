/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mumu.framework.core.cmd.enums.Cmd;

/**
 * CmdMapping
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 18:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CmdMapping {
    /** cmd */
    Cmd value();
}
