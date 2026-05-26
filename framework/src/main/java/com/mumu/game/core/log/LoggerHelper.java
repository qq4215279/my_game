/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import lombok.extern.slf4j.Slf4j;

/**
 * LoggerHelper
 * 日志帮助类
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:59
 */
@Slf4j
public class LoggerHelper {
    /** 异常堆栈行数 */
    static volatile int LINES = 100;
    /** BLANK */
    public static final String TAB = "\t";
    /** NEW_LINE */
    public static final String NEW_LINE = "\n";
    /** 异常关键串 */
    protected static final String EXCEPTION_STR = "com.mumu";

    public static void main(String[] args) {
        System.out.println("========== 测试1: getOriginThrowable 方法 ==========");
        testGetOriginThrowable();

        System.out.println("\n========== 测试2: getStackTrace 方法 ==========");
        testGetStackTrace();
    }

    /**
     * 测试 getOriginThrowable - 提取异常链的原始原因
     */
    private static void testGetOriginThrowable() {
        try {
            // 模拟反射调用产生的异常链
            invokeMethodWithException();
        } catch (Exception e) {
            System.out.println("【原始异常信息】:");
            System.out.println("异常类型: " + e.getClass().getName());
            System.out.println("异常消息: " + e.getMessage());
            System.out.println();

            // 使用 getOriginThrowable 提取根本原因
            Throwable origin = getOriginThrowable(e);
            System.out.println("【提取后的原始异常】:");
            System.out.println("异常类型: " + origin.getClass().getName());
            System.out.println("异常消息: " + origin.getMessage());
            System.out.println();

            System.out.println("【对比说明】:");
            System.out.println("外层包装: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                System.out.println("中间层: " + e.getCause().getClass().getSimpleName());
            }
            System.out.println("根本原因: " + origin.getClass().getSimpleName() + " <- 这才是真正需要关注的异常");
        }
    }

    /**
     * 模拟反射调用，产生 InvocationTargetException
     */
    private static void invokeMethodWithException() throws Exception {
        java.lang.reflect.Method method = LoggerHelper.class.getDeclaredMethod("businessMethod");
        method.invoke(null); // 这会抛出 InvocationTargetException
    }

    /**
     * 业务方法，抛出真正的异常
     */
    private static void businessMethod() {
        throw new NullPointerException("数据库查询结果为空，无法处理用户请求");
    }

    /**
     * 测试 getStackTrace - 获取线程调用堆栈
     */
    private static void testGetStackTrace() {
        // 获取当前线程的堆栈信息
        String stackTrace = LogTopic.getStackTrace();

        System.out.println("【当前线程调用堆栈】:");
        System.out.println(stackTrace);

        System.out.println("\n【说明】:");
        System.out.println("- 从索引3开始（跳过getStackTrace、testGetStackTrace、main）");
        System.out.println("- 显示代码调用的完整路径");
        System.out.println("- 用于追踪问题发生时的调用链路");
    }








    public void info(String msg, Throwable t) {
        // TODO 打印日志
        log.error(getThrowableTrace(msg, getOriginThrowable(t)));
    }


    /**
     * 获取错误的原始原因
     * 功能概述
     * 该方法用于递归提取嵌套异常的原始原因，解决异常包装导致的"异常链"问题，获取真正需要关注的底层异常。
     * 使用场景
     * 反射调用场景：处理 InvocationTargetException（反射调用时目标方法抛出的异常会被包装）
     * 动态代理场景：处理 UndeclaredThrowableException（动态代理中未声明的异常会被包装）
     * 日志记录：在 info() 方法中使用（第38行），打印异常时显示根本原因而非包装层
     * @param t t
     * @return java.lang.Throwable
     * @since 2024/12/5 22:59
     */
    public static Throwable getOriginThrowable(Throwable t) {
        // 获取目标异常
        if (t instanceof InvocationTargetException) {
            InvocationTargetException e = (InvocationTargetException)t;
            return getOriginThrowable(e.getTargetException());

            // 获取未声明异常c
        } else if (t instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException e = (UndeclaredThrowableException)t;
            return getOriginThrowable(e.getUndeclaredThrowable());

            // 处理普通 RuntimeException 且其 cause 为上述两种类型时继续递归c
        } else if (t.getClass() == RuntimeException.class) {
            RuntimeException e = (RuntimeException)t;
            if (null != e.getCause() && (e.getCause() instanceof InvocationTargetException
                    || e.getCause() instanceof UndeclaredThrowableException)) {
                return getOriginThrowable(e.getCause());
            }
        }

        return t;
    }

    /**
     * 获取异常Trace第num条
     * 
     * @param msg msg
     * @param arg1 arg1
     * @return java.lang.String
     * @since 2024/12/5 23:00
     */
    public static String getThrowableTrace(String msg, Throwable arg1) {
        int num = LINES;
        StackTraceElement[] stacks = arg1.getStackTrace();
        StringBuilder builder = new StringBuilder(256);
        builder.append(arg1);
        builder.append("#");
        if (null != msg) {
            builder.append(msg.replace("#", "@_@"));
        }

        int index = 1;
        boolean count = false;
        for (StackTraceElement element : stacks) {
            String value = element.toString();
            builder.append(NEW_LINE).append(TAB).append(value);
            if (!count && value.indexOf(EXCEPTION_STR) != -1) {
                // 如果出现了标志行
                count = true;
            }
            if (count) {
                // 计数
                index++;
                if (index > num) {
                    // 超过指定行
                    break;
                }
            }
        }
        return builder.toString();
    }
}
