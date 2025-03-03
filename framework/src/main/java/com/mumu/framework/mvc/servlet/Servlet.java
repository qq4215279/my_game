package com.mumu.framework.mvc.servlet;

/**
 * Servlet
 * 处理器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:04
 */
public interface Servlet {

    /**
     * 初始化
     */
    void init();

    /**
     * 处理请求
     */
    void service(Request request, Response response);

    /**
     *
     */
    void destroy();
}
