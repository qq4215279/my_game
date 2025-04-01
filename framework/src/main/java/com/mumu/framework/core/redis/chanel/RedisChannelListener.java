/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.redis.chanel;

import com.mumu.framework.core.redis.constants.RedisChannel;
import com.mumu.framework.core.util2.ModifierUtil;

/**
 * RedisChannelListener
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:35
 */
public interface RedisChannelListener<T> {

  /** 订阅到消息 */
  void onMessage(String channel, T message);

  /** 订阅目标渠道 */
  RedisChannel subscribeChannel();

  default Class<T> getTargetClazz() {
    return ModifierUtil.getGenericInterfaceClass(this.getClass(), RedisChannelListener.class, null);
  }

}
