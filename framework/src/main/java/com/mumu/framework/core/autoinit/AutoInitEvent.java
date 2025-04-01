/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.autoinit;

import com.mumu.framework.core.auto.AutoConditional;
import com.mumu.framework.core.autoinit.enums.AutoInitModule;

/**
 * AutoInitEvent
 * 初始化事件
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:11
 */
public interface AutoInitEvent extends AutoConditional {
  /** 自动初始化逻辑 */
  void autoInit();

  /** 所属模块 */
  default AutoInitModule getInitGroup() {
    return AutoInitModule.DEFAULE;
  }
}
