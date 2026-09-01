package com.mumu.game.core.scroll;

import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.core.util.ArrayUtil;
import com.mumu.game.core.clock.util.TimeUtil;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.redis.RedisUtil;
import com.mumu.game.core.redis.constants.RedisChannel;
import com.mumu.game.core.redis.constants.RedisKey;
import com.mumu.game.proto.message.core.ErrorCode;
import lombok.Builder;
import lombok.Data;

/**
 * <b>跑马灯消息构建类</b>
 *
 * <p>使用方法：
 *
 * <pre>{@code
 * ScrollParams.builder().type(ScrollType.GIFT).tmpKey(ScrollTmpKey.GIFT).args(List.of("111", "222")).build().send();
 * }</pre>
 *
 * @since 2024/11/5 @Author: xu.hai
 */
@Data
@Builder
public class ScrollParams {
  /** 跑马灯ID */
  private final long id = SnowflakeID.nextId();

  /** 类型 */
  private ScrollType type;

  /** 模板key */
  private String tmpKey;

  /** 模板参数 */
  private List<String> args;

  /** 优先级 */
  private int order;

  /** 播放开始时间ms */
  private Long beginTime;

  /** 播放结束时间ms */
  private Long endTime;

  /** 轮播间隔s */
  private Integer interval;

  /** 设置参数 */
  public ScrollParams args(Object... args) {
    if (ArrayUtil.isEmpty(args)) return this;
    List<String> argsList = Lists.newArrayListWithCapacity(args.length);
    for (Object arg : args) {
      argsList.add(String.valueOf(arg));
    }
    this.args = argsList;
    return this;
  }

  /** 获取Client跑马灯对象 */
  public ScrollBean toScroll() {
    ScrollBean scrollBean = new ScrollBean();
    scrollBean.setId(id);
    scrollBean.setType(type);
    scrollBean.setTmpKey(tmpKey);
    scrollBean.setArgs(args);
    scrollBean.setOrder(order);
    scrollBean.setBeginTime(beginTime);
    scrollBean.setEndTime(endTime);
    scrollBean.setInterval(interval);
    return scrollBean;
  }

  /** 发送跑马灯（广播消息，谨慎使用！） */
  public void send() {
    OnACScrollMessage pushMsg = new OnACScrollMessage();
    pushMsg.getScrolls().add(toScroll());
    MessageSender.broadcast(Cmd.OnACScroll, ErrorCode.SUCCESS, pushMsg);

    recordIntervalScroll();
  }

  /** 记录周期性跑马灯信息（即推送到 Redis、同时通知大厅服记录） */
  private void recordIntervalScroll() {
    // 一次性类型，无需写入redis
    if (type == ScrollType.SCROLL_COMMON || endTime == null || interval == null) return;
    // 已过期，也无需写入
    long expired = (endTime - System.currentTimeMillis()) / TimeUtil.ONE_SECOND_MILLIS;
    if (expired <= interval) return;

    // 记录跑马灯详情
    RedisUtil.set(RedisKey.SCROLL_INTERVAL.buildKey(id), this, expired);
    // 记录跑马灯ID集合
    RedisUtil.zadd(RedisKey.SCROLL_INTERVAL_IDS.buildKey(), id, endTime);
    // 通知跑马灯监听器
    RedisChannel.SCROLL_INTERVAL.publish(this);
  }
}
