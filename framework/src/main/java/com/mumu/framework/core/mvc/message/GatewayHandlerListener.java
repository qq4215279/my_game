/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.framework.core.mvc.servlet.session.SessionManager;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * GatewayHandlerListener
 * 网关的消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
@Component
public class GatewayHandlerListener extends AbstractHandlerListener {

    @Resource
    SessionManager sessionManager;
}
