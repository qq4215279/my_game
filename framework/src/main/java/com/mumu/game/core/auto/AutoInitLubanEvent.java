package com.mumu.game.core.auto;


import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.autoluban.autoluban2.AutoLubanEvent;
import com.mumu.game.core.log.LogTopic;

/** 自动初始化和鲁班监听回调接口 @Date: 2024/8/5 下午3:30 @Author: xu.hai */
public interface AutoInitLubanEvent<T>
    extends AutoInitEvent, AutoLubanEvent<T> {

  /** 启动初始化 和 鲁班监听回调，都会调用此方法 */
  void autoLoad();

  @Override
  default void autoInit() {
    Class<? extends AutoInitLubanEvent> aClass = getClass();
    String name = aClass.getSimpleName();
    autoLoad();
    LogTopic.ACTION.info("初始化鲁班配置表", name, " 完成!");
  }

  @Override
  default void autoLubanRefresh() {
    Class<? extends AutoInitLubanEvent> aClass = getClass();
    String name = aClass.getSimpleName();
    autoLoad();
    LogTopic.ACTION.info("刷新鲁班配置表", name, " 完成!");
  }

  /** 所属模块 */
  default AutoInitModule getInitGroup() {
    return AutoInitModule.LUBAN_DEFALUT_CONFIG;
  }
}
