package com.mumu.framework.core.redis.constants;

import com.mumu.framework.core.redis.RedisUtil;
import lombok.Getter;

/**
 * RedisChannel redis 发布订阅渠道
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:38
 */
@Getter
public enum RedisChannel {
  /** 玩家位置变动主题 */
  PLAYER_POSITION_CHANGE(SerializerType.PROTOBUF),
  /** 缓存模型数据变更 */
  AUTO_MODE_CHANGE(SerializerType.PROTOBUF),
  /** 系统邮件 */
  SYSTEM_MAIL(SerializerType.JSON),
  /** 轮播跑马灯 */
  SCROLL_INTERVAL(SerializerType.JSON),
  /** 广播消息 */
  BROADCAST(SerializerType.PROTOBUF),
  /** 比赛单局打完消息 */
  TOURNAMENT_GAME_OVER(SerializerType.JSON),
  /** 停服通知 */
  CLOSE_SERVER_NOTICE(SerializerType.JSON),
  /** 任务事件 */
  TASK(SerializerType.PROTOBUF),
  ;

  /** 序列化类型 */
  private final SerializerType serializer;

  RedisChannel(SerializerType serializer) {
    this.serializer = serializer;
  }

  /** 获取渠道key */
  public String getChannel() {
    return RedisKey.CHANNEL.buildKey(this.name());
  }

  /** 发布消息 */
  public void publish(Object message) {
    RedisUtil.publish(getChannel(), serializer.serialize(message));
  }
}
