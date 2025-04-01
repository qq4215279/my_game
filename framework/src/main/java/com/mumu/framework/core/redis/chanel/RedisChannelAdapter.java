/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.redis.chanel;

import java.util.List;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.redis.constants.RedisChannel;

import cn.hutool.core.util.StrUtil;

/**
 * RedisChannelAdapter
 * Redis发布订阅渠道监听适配器
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:46
 */
public class RedisChannelAdapter implements MessageListener {

  final List<RedisChannelListener> listeners;

  public RedisChannelAdapter(List<RedisChannelListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    RedisChannelListener listener = listeners.getFirst();
    RedisChannel channel = listener.subscribeChannel();
    Class targetClazz = listener.getTargetClazz();
    try {
      Object msg = channel.getSerializer().deserialize(message.getBody(), targetClazz);
      if (msg != null) {
        String channelStr = new String(message.getChannel());
        listeners.forEach(l -> l.onMessage(channelStr, msg));
      }
    } catch (Exception e) {
      LogTopic.ACTION.error(e, StrUtil.format("[{}] Redis channel adapter error！listeners: {}, target: {}, msg: {}",
          channel,
          listeners.size(),
          targetClazz,
          message.getBody()));
    }
  }

}
