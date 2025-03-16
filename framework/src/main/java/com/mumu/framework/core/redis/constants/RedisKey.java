package com.mumu.framework.core.redis.constants;

import cn.hutool.core.util.StrUtil;

/**
 * RedisKey Redis key 枚举类 - 用于构建redis key
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:42
 */
public enum RedisKey {
  /** 账号服token 信息（注意，此key游戏服与账号服共同使用，不要随意修改Key名称） */
  ACCOUNT_TOKEN("account:token:{}", RedisTypeEnum.STRING),
  /** 玩家语言编码集合 */
  PLAYER_LANGUAGE("player:language", RedisTypeEnum.HASH),

  /** 玩家昵称集合 */
  PLAYER_NICK("player:nick", RedisTypeEnum.SET),
  /** 玩家位置信息 %d-玩家id filed-serverGroup val-serverId */
  PLAYER_POSITION("player:position:{}", RedisTypeEnum.HASH),
  /** 玩家id集合（注意，此key游戏服与账号服共同使用，不要随意修改Key名称） */
  PLAYER_ID_BITSET("player:id:bitset", RedisTypeEnum.STRING),

  /** 全量机器人集合 */
  PLAYER_ROBOT_SET("player:robot:ids", RedisTypeEnum.SET),
  /** 机器人创建锁 %d-id */
  PLAYER_ROBOT_LOCK("player:robot:lock:{}", RedisTypeEnum.STRING),
  /** 可用的机器人池子(按金币排序) */
  PLAYER_ROBOT_POOL("player:robot:pool", RedisTypeEnum.ZSET),

  /** 发布订阅渠道 */
  CHANNEL("channel:{}", RedisTypeEnum.STRING),

  /** DB表结构自动更新分布锁 %s-DS名 %s-表名 */
  DB_SCHEMA_LOCK("schema_lock:{}:{}", RedisTypeEnum.STRING),

  /** DB表数据缓存（单个玩家一条） %s-表名 field-玩家ID */
  AUTO_MODEL_CACHE_ONE("model:{}", RedisTypeEnum.HASH),
  /** DB表数据缓存（单个玩家多条） %s-表名 %d-玩家ID field-主键 */
  AUTO_MODEL_CACHE_MANY("model:{}:{}", RedisTypeEnum.HASH),

  /** 系统邮件 %d-邮件ID */
  MAIL_SYSTEM("mail:system:{}", RedisTypeEnum.STRING),
  /** 系统邮件ID集合 */
  MAIL_SYSTEM_IDS("mail:system", RedisTypeEnum.ZSET),
  /** 玩家邮件 %d-玩家id field-邮件id */
  MAIL_PLAYER("mail:player:{}", RedisTypeEnum.HASH),
  /** 邮件锁 %s-唯一key */
  MAIL_LOCK("mail:lock:%s", RedisTypeEnum.STRING),

  /** 周期性跑马灯 %d-跑马灯ID */
  SCROLL_INTERVAL("scroll:interval:{}", RedisTypeEnum.STRING),
  /** 周期性跑马灯ID集合(score为过期时间s) */
  SCROLL_INTERVAL_IDS("scroll:interval", RedisTypeEnum.ZSET),

  /** 组成员集合 %s-groupId */
  GROUP_MEMBER_KEY("group:member:{}", RedisTypeEnum.SET),
  /** 组的ID key集合 %s-groupType */
  GROUP_ID_KEYS("group:id:{}", RedisTypeEnum.SET),

  /** 聊天消息ID生成序列 field：groupId val：position */
  CHAT_GROUP_POSITION("chat:group_position", RedisTypeEnum.HASH),

  /** 排行榜相关 */
  RANK("rank:{}", RedisTypeEnum.ZSET),

  /** 比赛玩家积分榜 %d-比赛id %d-比赛key */
  TOURNAMENT_PLAYER_RANK("tournament:{}:{}", RedisTypeEnum.ZSET),
  /** 比赛玩家积分榜（临时榜） %d-比赛id %d-比赛key */
  TOURNAMENT_PLAYER_RANK_TMP("tournament:{}:{}:tmp", RedisTypeEnum.ZSET),
  ;

  /** key 模板 */
  private final String template;

  /** redisType 对应 redis 数据类型，仅用于声明 */
  RedisKey(String template, RedisTypeEnum redisType) {
    this.template = template;
  }

  /**
   * 获取 redis key
   *
   * @param params 构造key所需参数集
   * @return redis key
   */
  public String buildKey(Object... params) {
    return StrUtil.format(this.template, params);
  }

  /** redis 数据类型 */
  private enum RedisTypeEnum {
    STRING,
    HASH,
    LIST,
    SET,
    ZSET,
  }
}
