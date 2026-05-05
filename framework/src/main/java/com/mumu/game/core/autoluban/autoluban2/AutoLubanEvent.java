package com.mumu.game.core.autoluban.autoluban2;

import com.cxx.luban.lubanconfigclient.AbLubanConfigLoaderClient;
import com.cxx.luban.lubanconfigclient.ILubanConfigClient;
import com.game.framework.core.auto.AutoConditional;
import com.game.framework.core.utils.ModifierUtil;
import com.game.framework.core.utils.SpringContextUtils;

/** 自动鲁班监听回调事件 @Date: 2024/8/5 上午10:21 @Author: xu.hai */
public interface AutoLubanEvent<T extends AbLubanConfigLoaderClient> extends AutoConditional {

  /** 鲁班回调刷新 */
  void autoLubanRefresh();

  /** 获取鲁班 Loader */
  default T getLubanLoader() {
    Class<T> lubanLoaderType = getLubanLoaderType();
    if (lubanLoaderType == null) return null;
    return SpringContextUtils.getBean(ILubanConfigClient.class)
        .getConfigConvertLoader(lubanLoaderType);
  }

  /** 获取鲁班 Loader 类型 */
  default Class<T> getLubanLoaderType() {
    Class<?> origin = this.getClass();
    if (ModifierUtil.isProxyClass(origin)) origin = origin.getSuperclass();
    return ModifierUtil.getGenericInterfaceClass(origin, null, AbLubanConfigLoaderClient.class);
  }
}
