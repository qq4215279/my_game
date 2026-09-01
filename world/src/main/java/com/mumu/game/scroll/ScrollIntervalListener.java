package com.mumu.game.scroll;

import com.mumu.game.core.redis.chanel.RedisChannelListener;
import com.mumu.game.core.redis.constants.RedisChannel;
import com.mumu.game.core.scroll.ScrollParams;
import org.springframework.stereotype.Component;


import jakarta.annotation.Resource;

/** 周期性跑马灯监听器 @Date: 2025/1/21 下午2:53 @Author: xu.hai */
@Component
public class ScrollIntervalListener implements RedisChannelListener<ScrollParams> {

  @Resource ScrollManager scrollManager;

  @Override
  public void onMessage(String channel, ScrollParams params) {
    scrollManager.tryAddScroll(params);
  }

  @Override
  public RedisChannel subscribeChannel() {
    return RedisChannel.SCROLL_INTERVAL;
  }
}
