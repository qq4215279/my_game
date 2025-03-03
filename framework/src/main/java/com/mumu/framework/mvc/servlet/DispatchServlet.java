package com.mumu.framework.mvc.servlet;

import lombok.Data;

/**
 * DispatchServlet
 *
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:06
 */
@Data
public class DispatchServlet implements Servlet {

    private TokenBody tokenBody;

    @Override
    public void init() {

    }

    @Override
    public void service(Request request, Response response) {

    }

    @Override
    public void destroy() {

    }
}
