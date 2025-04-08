/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.message;

import com.mumu.framework.core.log.LogTopic;

/**
 * AbstractHandlerListener
 * 抽象消息处理监听器
 * @author liuzhen
 * @version 1.0.0 2025/4/1 21:35
 */
public abstract class AbstractHandlerListener implements MessageHandlerListener {
    protected final static LogTopic log = LogTopic.ACTION;

    public AbstractHandlerListener() {
    }

}
