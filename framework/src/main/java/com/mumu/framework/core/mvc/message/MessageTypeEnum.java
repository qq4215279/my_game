package com.mumu.framework.core.mvc.message;

/**
 * MessageTypeEnum
 * 消息类型枚举
 * @author liuzhen
 * @version 1.0.0 2025/3/3 22:40
 */
public enum MessageTypeEnum {
    /** 客户端请求消息 */
    REQUEST,
    /** 客户端响应消息 */
    RESPONSE,
    /** RPC请求消息 */
    RPC_REQUEST,
    /** RPC响应消息 */
    RPC_RESPONSE
}
