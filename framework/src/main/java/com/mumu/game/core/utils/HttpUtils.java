package com.mumu.game.core.utils;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;

import com.game.framework.core.consts.ThreadConstants;
import com.game.framework.core.log.LogTopic;
import com.game.framework.core.thread.ThreadSwitchWrapper;
import com.game.http.core.HttpResult;
import com.game.util.JsonUtil;

import cn.hutool.http.HttpUtil;

/** http请求工具类 @Date: 2024/8/12 20:21 @Author: xu.hai */
public class HttpUtils {
  public static ThreadPoolExecutor executor =
      ThreadPoolUtil.newExecutor(ThreadConstants.THREAD_PREFIX_HTTP, 8, 16, 30000, 1000);

  /** 异步 Get请求 */
  public static void asyncGet(
      String url, Map<String, Object> paramMap, BiConsumer<String, Exception> consumer) {
    executor.execute(ThreadSwitchWrapper.wrap(() -> get(url, paramMap, consumer)));
  }

  /** 同步 Get请求 */
  public static void get(
      String url, Map<String, Object> paramMap, BiConsumer<String, Exception> consumer) {
    String result = null;
    Exception exception = null;
    try {
      result = HttpUtil.get(url, paramMap);
    } catch (Exception e) {
      exception = e;
      LogTopic.ACTION.error(e, "http请求异常", "url", url, "paramMap", paramMap, "result", result);
    }
    try {
      consumer.accept(result, exception);
    } catch (Exception e) {
      LogTopic.ACTION.error(
          e, "http请求回调执行异常", "url", url, "paramMap", paramMap, "result", result, "e", exception);
    }
  }

  public static void asyncGetHttpResult(
      String url, Map<String, Object> paramMap, BiConsumer<HttpResult, Exception> consumer) {
    executor.execute(ThreadSwitchWrapper.wrap(() -> getHttpResult(url, paramMap, consumer)));
  }

  public static void getHttpResult(
      String url, Map<String, Object> paramMap, BiConsumer<HttpResult, Exception> consumer) {
    get(
        url,
        paramMap,
        (result, exception) ->
            consumer.accept(JsonUtil.fromJson(result, HttpResult.class), exception));
  }
}
