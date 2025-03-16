package com.mumu.framework.core.autoinit;

import com.mumu.framework.core.autoinit.enums.AutoInitModule;

/**
 * AutoInitEvent
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:11
 */
public interface AutoInitEvent {
  /** 自动初始化逻辑 */
  void autoInit();

  /** 排序（小的优先执行） */
  default int order() {
    return 10;
  }

  /** 所属模块 */
  default AutoInitModule getInitGroup() {
    return AutoInitModule.DEFAULE;
  }
}
