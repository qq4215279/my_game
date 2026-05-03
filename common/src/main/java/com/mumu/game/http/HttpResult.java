package com.mumu.game.http;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import lombok.Data;

/**
 * HttpResult
 * 构建返回
 * @author liuzhen
 * @version 1.0.0 2024/11/28 11:39
 */
@Data
public class HttpResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private int code = HttpCode.SUCCESS;
    /** 错误码 */
    private String msg = "success";
    /** data */
    private Map<String, Object> data = new HashMap<>();

    private static HttpResult of() {
        return new HttpResult();
    }

    /**
     * 创建成功返回
     *
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:55
     */
    public static HttpResult success() {
        return of();
    }

    /**
     * 创建成功返回
     *
     * @param msg msg
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:55
     */
    public static HttpResult success(String msg) {
        return of().setMsg(msg);
    }

    /**
     * 创建成功返回
     */
    public static HttpResult success(Map<String, ?> data) {
        return of().add(data);
    }

    /**
     * 创建成功返回
     *
     * @param msg  msg
     * @param data data
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:55
     */
    public static HttpResult success(String msg, Map<String, Object> data) {
        return of().setMsg(msg).add(data);
    }

    /**
     * 创建失败返回
     *
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:55
     */
    public static HttpResult error() {
        return error(HttpCode.SERVER_EXCEPTION, "未知异常，请联系管理员");
    }

    /**
     * 创建失败返回
     *
     * @param msg msg
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:56
     */
    public static HttpResult error(String msg) {
        return error(HttpCode.SERVER_EXCEPTION, msg);
    }

    /**
     * 创建失败返回
     *
     * @param code code
     * @param msg  msg
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:56
     */
    public static HttpResult error(int code, String msg) {
        HttpResult httpResult = of();
        httpResult.code = code;
        httpResult.msg = msg;

        return httpResult;
    }

    /**
     * 追加返回值
     */
    public HttpResult add(Map<String, ?> data) {
        data.forEach(this::add);
        return this;
    }

    /**
     * 添加 data值
     */
    public HttpResult add(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 添加 data值（value对象会被提前序列化json字符串，避免接收方解为HttpResult对象时，无法解析为指定对象）
     */
    public HttpResult addStr(String key, Object value) {
        return this.add(key, JsonUtil.toJson(value));
    }

    /**
     * 追加返回值
     */
    public HttpResult addStr(Map<String, ?> data) {
        data.forEach(this::addStr);
        return this;
    }

    /**
     * 设置msg
     *
     * @param msg msg
     * @return com.game.framework.http.common.HttpResult
     * @since 2024/11/28 11:54
     */
    public HttpResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    /**
     * 请求是否成功
     */
    public boolean isOk() {
        return code == HttpCode.SUCCESS;
    }

    /**
     * 针对明确的key类型才能直接使用
     */
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        return JsonUtil.fromJson(get(key), type);
    }

    public <T> T get(String key, TypeToken<T> type) {
        return JsonUtil.fromJson(get(key), type);
    }

    public <K, V> Map<K, V> getMap(String key, Class<K> k, Class<V> v) {
        return JsonUtil.fromJsonMap(get(key), k, v);
    }

    public <T> List<T> getList(String key, Class<T> type) {
        return JsonUtil.fromJsonList(get(key), type);
    }

}
