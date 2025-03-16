package com.mumu.framework.core.redis.constants;

import java.util.concurrent.TimeUnit;

/**
 * RedisConstants
 * Redis常量
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:36
 */
public interface RedisConstants {
  /** lua 脚本前缀 */
  String LUA_SCRIPT_PREFIX = "lua/";

  /** lua 脚本后缀 */
  String LUA_SCRIPT_SUFFIX = ".lua";

  /** 获取 lua 脚本相对路径 */
  static String getLuaScriptPath(String fileName) {
    return LUA_SCRIPT_PREFIX + fileName + LUA_SCRIPT_SUFFIX;
  }

  /** 一天的秒数 */
  long ONE_DAY_SECOND = TimeUnit.DAYS.toSeconds(1);
}
