package com.mumu.common.constants;

/**
 * ThreadConstants
 * 线程相关常量
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:58
 */
public interface ThreadConstants {
  /** 线程名前缀 - 玩家线程池 */
  String THREAD_PREFIX_PLAYER = "player-worker-";

  /** 线程名前缀 - 服务线程池 */
  String THREAD_PREFIX_SERVER = "server-worker-";

  /** 线程名前缀 - 常规定时任务线程池 */
  String THREAD_PREFIX_SCHEDULED = "scheduled-worker-";

  /** 线程名前缀 - 缓存模型线程池 */
  String THREAD_PREFIX_CACHE_MODEL = "cache-model-executor-";

  /** 线程名前缀 - http线程池 */
  String THREAD_PREFIX_HTTP = "http-executor-";

  /** 线程名前缀 - 游戏同步线程池 */
  String THREAD_PREFIX_GAME_SYNC = "game-sync-executor-";

  /** 线程名前缀 - 游戏匹配线程池 */
  String THREAD_PREFIX_MATCH = "match-executor-";

  /** 缓存模型线程池 - 核心线程数 */
  int CACHE_MODEL_CORE_SIZE = 8;

  /** 匹配队列长度 */
  int MATCH_QUEUE_SIZE = 10000;
}
