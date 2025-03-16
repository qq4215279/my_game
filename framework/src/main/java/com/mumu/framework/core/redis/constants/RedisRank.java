package com.mumu.framework.core.redis.constants;

import com.mumu.framework.core.redis.RedisUtil;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

/**
 * RedisRank
 * 排行榜 KEY
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:43
 */
public enum RedisRank {
  /** 系统邮件ID榜（用于玩家加载系统邮件顺序） */
  SYSTEM_MAIL("system_mail"),
  /** 在线玩家推荐度 */
  ONLINE_RECOMMEND_POINTS("online_recommend_points"),
  /** 玩家周推荐度 */
  PLAYER_WEEK_RECOMMEND_POINTS("week_recommend_points:%d"),
  /** 拥有vip卡玩家 */
  PLAYER_VIP_CARD("vip:player_vip_card"),

  // ----------------------------- 排行榜key ----------------------------->
  /** 金币榜 %s-榜单类型（RankTypeEnum） %d-第几期 period */
  GOLD("gold:%s:%d"),
  /** VIP榜 %s-榜单类型（RankTypeEnum） %d-第几期 period */
  VIP("vip:%s:%d"),
  /** 经验榜 %s-榜单类型（RankTypeEnum） %d-第几期 period */
  EXP("exp:%s:%d"),
  /** 魅力榜 %s-榜单类型（RankTypeEnum） %d-第几期 period */
  CHARM("charm:%s:%d"),

// ----------------------------- 排行榜key ----------------------------->
  ;

  /** key 模板 */
  private final String template;

  RedisRank(String template) {
    this.template = template;
  }

  /**
   * 获取 redis key
   *
   * @param params 构造key所需参数集
   * @return redis key
   */
  public String buildKey(Object... params) {
    return RedisKey.RANK.buildKey(String.format(this.template, params));
  }

  /** 获取玩家排名积分信息 */
  public ScoreInfo getScoreInfo(long id, Object... params) {
    String key = buildKey(params);
    ScoreInfo info = new ScoreInfo(id);
    info.setRank((int) RedisUtil.getRank(key, id));
    info.setScore(RedisUtil.getScore(key, id));
    return info;
  }

  /** 获取玩家排名（第一名为1，-1表示未查到） */
  public long getRank(long id, Object... params) {
    return RedisUtil.getRank(buildKey(params), id);
  }

  /** 增加玩家积分（覆盖） */
  public boolean zadd(long id, long score, Object... params) {
    return RedisUtil.zadd(buildKey(params), id, score);
  }

  /** 增加玩家积分（累加） */
  public long incr(long id, long score, Object... params) {
    return RedisUtil.zIncr(buildKey(params), id, score, true);
  }

  /** 增加玩家积分（lua脚本加积分，积分相同按时间排序，并可设置榜单过期时间） */
  public void incrWithExpire(long id, long score, int expireTime, Object... params) {
    RedisUtil.incrWithExpire(buildKey(params), id, score, expireTime, true);
  }

  /** 移除指定玩家 */
  public long zremove(long id, Object... params) {
    return RedisUtil.zremove(buildKey(params), id);
  }

  /** 获取排名 TopN （rank-排名，从1开始） */
  public Set<TypedTuple<String>> getTopN(int rank, Object... params) {
    return RedisUtil.getTopN(buildKey(params), rank);
  }

  /** 获取排名 TopN （rank-排名，从1开始） */
  public Map<Long, ScoreInfo> getTopNMap(int rank, Object... params) {
    return RedisUtil.getTopNMap(buildKey(params), rank);
  }

  /** 获取大于指定积分的全部元素 */
  public Set<String> getElementsAboveScore(long score, Object... params) {
    return RedisUtil.getElementsAboveScore(buildKey(params), score);
  }
}
