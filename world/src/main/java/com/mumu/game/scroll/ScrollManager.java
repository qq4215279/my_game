package com.mumu.game.scroll;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.scroll.ScrollParams;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** 大厅跑马灯缓存管理类 @Date: 2024/11/5 下午4:28 @Author: xu.hai */
@Component
public class ScrollManager implements AutoInitEvent {

  /** 缓存持续一段时间的跑马灯 */
  private final Map<Long, ScrollParams> scrollMap = Maps.newConcurrentMap();

  @Override
  public void autoInit() {
    // 冗余20s
    long offSet = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
    // 移除过期的跑马灯ID
    RedisUtil.removeRangeByScore(RedisKey.SCROLL_INTERVAL_IDS.buildKey(), 0, offSet);
    // 获取在有效期的跑马灯ID集合
    Set<String> scrollIds =
        RedisUtil.getElementsAboveScore(RedisKey.SCROLL_INTERVAL_IDS.buildKey(), offSet);

    LogTopic.ACTION.info("Scroll interval load", "offSet", offSet, "scrollIds", scrollIds);
    // 根据跑马灯ID获取跑马灯详情
    scrollIds.stream()
        .map(
            scrollId ->
                RedisUtil.get(RedisKey.SCROLL_INTERVAL.buildKey(scrollId), ScrollParams.class))
        .filter(Objects::nonNull)
        .forEach(params -> scrollMap.put(params.getId(), params));
  }

  /** 玩家登录，推送持续性的跑马灯 */
  @EventListener(OnlineEvent.class)
  public void handlerScrollEvent(OnlineEvent event) {
    Player player = event.getPlayer();

    // 获取全部待推送的跑马灯
    long now = System.currentTimeMillis();
    List<ScrollBean> scrolls = Lists.newArrayList();
    scrollMap.forEach(
        (k, v) -> {
          if (v.getEndTime() > now) scrolls.add(v.toScroll());
          else scrollMap.remove(k);
        });

    if (scrolls.isEmpty()) return;
    // 推送给玩家
    OnACScrollMessage pushMsg = new OnACScrollMessage();
    pushMsg.setScrolls(scrolls);
    MessageSender.sendToPlayer(player.getPlayerId(), Cmd.OnACScroll, pushMsg);
  }

  /** 添加周期性的跑马灯 */
  public void tryAddScroll(ScrollParams params) {
    if (params.getEndTime() != null
        && params.getEndTime().compareTo(System.currentTimeMillis()) > 0)
      scrollMap.put(params.getId(), params);
  }

  /** 尝试删除周期性的跑马灯 */
  public void tryDelScroll(List<Long> ids) {
    for (long id : ids) {
      // 移除跑马灯缓存
      scrollMap.remove(id);
      // 移除跑马灯Redis集合
      RedisUtil.zremove(RedisKey.SCROLL_INTERVAL_IDS.buildKey(), id);
      // 移除跑马灯Redis详情
      RedisUtil.del(RedisKey.SCROLL_INTERVAL.buildKey(id));
    }
  }
}
