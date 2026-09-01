package com.mumu.game.core.scroll;

import java.util.List;

import com.mumu.game.core.clock.util.TimeUtil;
import lombok.Getter;

/** 跑马灯消息模板key @Date: 2024/11/6 下午5:12 @Author: xu.hai */
@Getter
public enum ScrollTmpKey {


  /** 【升级】恭喜玩家%s升级到%s级 */
  UPGRADE("scroll_upgrade"),

  /** 【榜单前3登陆】玩家%s进入游戏 */
  WELCOME("scroll_welcome"),

  /** 【baloot】恭喜玩家%s打出春天 */
  BALOOT_SPRING("scroll_spring"),

  ;

  private final String key;

  ScrollTmpKey(String key) {
    this.key = key;
  }

  /** 发送跑马灯消息 */
  public void send(String... params) {
    ScrollParams.builder()
        .type(ScrollType.SCROLL_COMMON)
        .tmpKey(key)
        .args(List.of(params))
        .build()
        .send();
  }

  /** 发送轮播跑马灯消息 */
  public void send(int interval, int count, String... params) {
    send(System.currentTimeMillis(), interval, count, params);
  }

  /**
   * 发送轮播跑马灯消息
   *
   * @param beginTime 开始时间
   * @param interval 轮播周期
   * @param count 轮播次数
   * @param params 模板参数
   */
  public void send(long beginTime, int interval, int count, String... params) {
    // 开始时间 + 轮播间隔 * 轮播次数
    long endTime = beginTime + interval * TimeUtil.ONE_SECOND_MILLIS * count;
    ScrollParams.builder()
        .type(ScrollType.SCROLL_SYSTEM)
        .tmpKey(key)
        .args(List.of(params))
        .beginTime(beginTime)
        .endTime(endTime)
        .interval(interval)
        .build()
        .send();
  }
}
