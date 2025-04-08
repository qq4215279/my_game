/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.server;

import com.alibaba.fastjson2.JSONObject;
import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cmd.enums.Cmd;
import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.util.JProtoBufUtil;

import io.netty.util.AttributeKey;
import lombok.Getter;

/**
 * MessageContext
 * 消息上下文
 * @author liuzhen
 * @version 1.0.0 2025/4/8 21:45
 */
@Getter
public class MessageContext {
    /** 消息代理 */
    private GameMessagePackage proxy;

    /** 消息来源 session */
    private transient IoSession session;


    /** 获取消息 */
    public <T> T getMsg(Class<T> clazz) {
        return JProtoBufUtil.decode(proxy.getBody(), clazz);
    }

    /** 获取请求消息 */
    @SuppressWarnings("unchecked")
    public <T> T getReqMsg() {
        Cmd cmd = getCmd();
        return cmd != null && cmd.getReqMsgClass() != null ? (T) getMsg(cmd.getReqMsgClass()) : null;
    }

    /** 获取响应消息 */
    @SuppressWarnings("unchecked")
    public <T> T getResMsg() {
        Cmd cmd = getCmd();
        return cmd != null && cmd.getResMsgClass() != null ? (T) getMsg(cmd.getResMsgClass()) : null;
    }

    /** 获取玩家ID */
    public Long getPlayerId() {
        return proxy.getHeader().getPlayerId();
    }

    /** 获取 Cmd Code */
    public int getCmdCode() {
        return proxy.getHeader().getMessageId();
    }

    /** 获取 Cmd */
    public Cmd getCmd() {
        return CmdManager.getCmd(proxy.getHeader().getMessageId());
    }

    /** 获取 Seq */
    public int getSeq() {
        return proxy.getHeader().getClientSeqId();
    }

    /** 判断连接上是否存在指定 key 的属性 */
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return session.hasAttr(key);
    }

    /** 获取连接上指定 key 的属性 */
    public <T> T getAttr(AttributeKey<T> key) {
        return session.getAttr(key);
    }

    /** 获取连接上指定 key 的属性，没有则返回默认值 */
    public <T> T getAttr(AttributeKey<T> key, T orElse) {
        return session.getAttr(key, orElse);
    }

    /** 构造消息上下文 */
    public static MessageContext of(GameMessagePackage proxy, IoSession session) {
        MessageContext context = new MessageContext();
        context.proxy = proxy;
        context.session = session;
        return context;
    }

    @Override
    public String toString() {
        long begin = System.nanoTime();
        JSONObject json = new JSONObject();
        GameMessageHeader header = proxy.getHeader();
        json.put("playerId", header.getPlayerId());
        json.put("messageId", header.getMessageId());
        json.put("seq", header.getClientSeqId());
        json.put("errorCode", header.getErrorCode());
        if (session != null) {
            // json.put("session", session.session().toString());
            json.put("session", session.channel().toString());
        }

        Cmd cmd = getCmd();
        if (cmd != null) {
            json.put("cmd", cmd);
            if (cmd.getReqMsgClass() != null) {
                json.put("reqProtoName", cmd.getReqMsgClass().getSimpleName());
                json.put("reqMsg", getReqMsg());
            }

            if (cmd.getResMsgClass() != null) {
                json.put("resProtoName", cmd.getResMsgClass().getSimpleName());
                json.put("resMsg", getReqMsg());
            }
        }

        json.put("cost", System.nanoTime() - begin);
        return json.toString();
    }

    /** 打印Debug日志 */
    public void debug(String action) {
        // TODO
        /* if (getCmd() != null) {
            // 屏蔽的协议
            if (ConfigSystemParamsEnum.LOG_EXCLUDE_CMD.getSet().contains(getCmd().name())) return;
            // 查看的协议，空表示查看全部
            Set<String> includes = ConfigSystemParamsEnum.LOG_INCLUDE_CMD.getSet();
            if (!includes.isEmpty() && !includes.contains(getCmd().name())) return;
        }

        Long playerId = getPlayerId();
        if (playerId != null) {
            LogTopic.NET.debug(playerId, LogSwitch.CMD, action, "context", this);
        } else {
            LogTopic.NET.debug(LogSwitch.CMD, action, "context", this);
        } */
    }

    /** 打印异常日志 */
    public void error(String action) {
        LogTopic.NET.error(action, "context", this);
    }
}
