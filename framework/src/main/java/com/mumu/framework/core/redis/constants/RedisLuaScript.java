package com.mumu.framework.core.redis.constants;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import com.mumu.framework.core.redis.RedisUtil;
import java.util.List;
import lombok.Getter;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * RedisLuaScript lua 脚本定义
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:41
 */
public enum RedisLuaScript {
  /** 排行榜 zset 积分更新脚本 */
  ZSCORE_UPDATE_BY_TIME("z_score_update_by_time", Long.class),
  /** ID生成脚本（允许未来的ID被占用）（k1-计数器键,k2-位图键） */
  ID_GEN("id_gen", Long.class),
  /** 从zset中获取并移除一个范围中的N个元素 （k1-zsetKey,k2-minScore,k3-maxScore,k4-count） */
  ZSET_RANGE_POP("zset_range_pop", List.class),
  /** 从zset中批量获取元素排名和积分 （k1-zsetKey, args-members）,返回结果 List<ScoreInfo> */
  ZSET_GET_RANK_WITH_SCORE("zset_get_rank_with_score", String.class),
  ;

  /** 文件名 */
  private final String filePath;

  /** 返回值类型 {@link ReturnType#fromJavaType(Class)} */
  private final Class<?> resultTypeClass;

  @Getter
  private final ReturnType resultType;

  /** 预加载脚本 */
  private RedisScript<?> script;

  RedisLuaScript(String fileName) {
    this(fileName, null);
  }

  RedisLuaScript(String fileName, Class<?> resultTypeClass) {
    this.filePath = RedisConstants.getLuaScriptPath(fileName);
    this.resultTypeClass = resultTypeClass;
    this.resultType = ReturnType.fromJavaType(resultTypeClass);
  }

  /** 获取 lua脚本文本 */
  public String getScriptText() {
    return getScript().getScriptAsString();
  }

  /** 获取 lua脚本 sha */
  public String getScriptSha() {
    return getScript().getSha1();
  }

  /** 获取脚本 */
  @SuppressWarnings("unchecked")
  public <T> RedisScript<T> getScript() {
    if (script == null) {
      synchronized (this) {
        if (script == null) {
          String scriptText = ResourceUtil.readUtf8Str(filePath);
          Assert.notBlank(scriptText, "lua script is null: {}", filePath);
          script = new DefaultRedisScript<>(scriptText, resultTypeClass);
        }
      }
    }
    return (RedisScript<T>) script;
  }

  /** 在指定连接上执行脚本（param全为 key） */
  public <T> T executeLua(StringRedisConnection connection, String... params) {
    return connection.evalSha(getScriptSha(), getResultType(), params.length, params);
  }

  /** 执行lua脚本（简单的返回类型用此方法，复杂的返回类型，可用string-json） */
  public <T> T executeLua(List<String> keys, Object... args) {
    String[] params = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      params[i] = args[i].toString();
    }
    return RedisUtil.executeLua(this, keys, params);
  }
}
