package com.mumu.game.charge.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

/**
 * RestUtil
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 15:05
 */
@Slf4j
@Component
public class RestUtil {
    private static RestTemplate restTemplate;

    public RestUtil(RestTemplate restTemplate) {
        RestUtil.restTemplate = restTemplate;
    }

    /**
     * Get 请求，返回指定类型
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param responseType 响应体类型
     * @param uriVariables 参数列表，顺序替换
     * @return 指定类型结果
     * @param <T> 响应体类型
     */
    public static <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.getForObject(url, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil getForObject error! url: {}, uriVariables: {}, responseType: {}",
                    url,
                    Arrays.toString(uriVariables),
                    responseType,
                    e);
            return null;
        }
    }

    /**
     * Get 请求，返回指定类型
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param responseType 响应体类型
     * @param uriVariables 参数Map（k-url中占位符，v-值）
     * @return 指定类型结果
     * @param <T> 响应体类型
     */
    public static <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) {
        try {
            return restTemplate.getForObject(url, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil getForObject error! url: {}, uriVariables: {}, responseType: {}",
                    url,
                    uriVariables,
                    responseType,
                    e);
            return null;
        }
    }

    /**
     * Post 请求，返回指定类型
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param request 请求数据，可以为HttpEntity类型，或者其他json对象，或String
     * @param responseType 响应体类型
     * @param uriVariables 参数列表，顺序替换
     * @return 指定类型结果
     * @param <T> 响应体类型
     */
    public static <T> T postForObject(
            String url, Object request, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.postForObject(url, request, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil postForObject error! url: {}, uriVariables: {}, responseType: {}, request: {}",
                    url,
                    Arrays.toString(uriVariables),
                    responseType,
                    request,
                    e);
            return null;
        }
    }

    /**
     * Post 请求，返回指定类型
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param request 请求数据，可以为HttpEntity类型，或者其他json对象，或String
     * @param responseType 响应体类型
     * @param uriVariables 参数Map（k-url中占位符，v-值）
     * @return 指定类型结果
     * @param <T> 响应体类型
     */
    public static <T> T postForObject(
            String url, Object request, Class<T> responseType, Map<String, ?> uriVariables) {
        try {
            return restTemplate.postForObject(url, request, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil postForObject error! url: {}, uriVariables: {}, responseType: {}, request: {}",
                    url,
                    uriVariables,
                    responseType,
                    request,
                    e);
            return null;
        }
    }

    /**
     * 发起请求
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param requestEntity 请求实体（可设置请求头、请求体，可为null）
     * @param responseType 响应体类型
     * @param uriVariables 参数列表，顺序替换
     * @return 响应体
     * @param <T> 响应体类型
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            @Nullable HttpEntity<?> requestEntity,
            Class<T> responseType,
            Object... uriVariables) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil exchange error! url: {}, method: {}, uriVariables: {}, responseType: {}",
                    url,
                    method,
                    Arrays.toString(uriVariables),
                    responseType,
                    e);
            return null;
        }
    }

    /**
     * 发起请求
     *
     * @param url 如：http://localhost:8080/test/{id}/{token}，或
     *     http://localhost:8080/test?id={id}&token={token}
     * @param requestEntity 请求实体（可设置请求头、请求体，可为null）
     * @param responseType 响应体类型
     * @param uriVariables 参数Map（k-url中占位符，v-值）
     * @return 响应体
     * @param <T> 响应体类型
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            @Nullable HttpEntity<?> requestEntity,
            Class<T> responseType,
            Map<String, ?> uriVariables) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        } catch (Exception e) {
            log.error(
                    "RestUtil exchange error! url: {}, method: {}, uriVariables: {}, responseType: {}",
                    url,
                    method,
                    uriVariables,
                    responseType,
                    e);
            return null;
        }
    }
}
