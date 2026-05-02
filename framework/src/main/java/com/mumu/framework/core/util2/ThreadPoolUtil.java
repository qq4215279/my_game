/*
 * Copyright 2020-2026, mumu without 996. All Right Reserved.
 */

package com.mumu.framework.core.util2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mumu.framework.core.log.LogTopic;

import cn.hutool.core.thread.NamedThreadFactory;

/**
 * ThreadPoolUtil 线程池工具
 * 
 * @author liuzhen
 * @version 1.0.0 2024/8/15 17:11
 */
public class ThreadPoolUtil {

    /** 创建单线程池 */
    public static ThreadPoolExecutor newSingleExecutor(String threadNamePrefix, int maxQueueSize) {
        return newExecutor(threadNamePrefix, 1, 1, 0L, maxQueueSize);
    }

    /** 创建线程池 */
    public static ThreadPoolExecutor newExecutor(String threadNamePrefix, int corePoolSize, int maximumPoolSize,
        long keepAliveTime, int maxQueueSize) {
        return newExecutor(threadNamePrefix, corePoolSize, maximumPoolSize, keepAliveTime, maxQueueSize,
            newLogRejectedHandler(threadNamePrefix));
    }

    /** 创建线程池 */
    public static ThreadPoolExecutor newExecutor(String threadNamePrefix, int corePoolSize, int maximumPoolSize,
        long keepAliveTime, int maxQueueSize, RejectedExecutionHandler handler) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
            newBlockingQueue(maxQueueSize), newThreadFactory(threadNamePrefix), handler);
    }

    /** 创建定时任务线程池 */
    public static ScheduledExecutorService newScheduledExecutor(int corePoolSize, String threadNamePrefix) {
        return Executors.newScheduledThreadPool(corePoolSize, newThreadFactory(threadNamePrefix));
    }

    /** 创建线程工厂 */
    public static ThreadFactory newThreadFactory(String threadNamePrefix) {
        return new NamedThreadFactory(threadNamePrefix, false);
    }

    /** 创建阻塞队列 */
    public static <T> BlockingQueue<T> newBlockingQueue(int maxQueueSize) {
        return maxQueueSize > 0 ? new LinkedBlockingQueue<>(maxQueueSize) : new LinkedBlockingQueue<>();
    }

    /** 创建拒绝策略（打日志） */
    public static RejectedExecutionHandler newLogRejectedHandler(String threadNamePrefix) {
        return (r, c) -> LogTopic.ACTION.error("newLogRejectedHandler", threadNamePrefix,
            " execute fail, queue is full");
    }
}
