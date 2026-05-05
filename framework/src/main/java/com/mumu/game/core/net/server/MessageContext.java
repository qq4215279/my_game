/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.net.server;

import com.alibaba.fastjson2.JSONObject;
import com.mumu.game.core.cmd.enums.CmdManager;
import com.mumu.game.core.cmd.enums.ICmd;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.JProtoBufUtil;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

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
    /** 游戏消息包 */
    private GameMessagePackage messagePackage;
    /** 消息来源 session */
    private transient IoSession session;


    /** 获取请求消息 */
    @SuppressWarnings("unchecked")
    public <T> T getReqMsg() {
        ICmd cmd = getCmd();
        return cmd != null && cmd.getReqMsgClass() != null ? (T) getMsg(cmd.getReqMsgClass()) : null;
    }

    /** 获取响应消息 */
    @SuppressWarnings("unchecked")
    public <T> T getResMsg() {
        ICmd cmd = getCmd();
        return cmd != null && cmd.getResMsgClass() != null ? (T) getMsg(cmd.getResMsgClass()) : null;
    }

    /** 获取消息 */
    public  <T> T getMsg(Class<T> clazz) {
        return JProtoBufUtil.decode(messagePackage.getBody(), clazz);
    }

    /** 获取玩家ID */
    public Long getPlayerId() {
        return messagePackage.getHeader().getPlayerId();
    }

    /** 获取协议id */
    public int getMessageId() {
        return messagePackage.getHeader().getMessageId();
    }

    /** 获取 Cmd */
    public ICmd getCmd() {
        return CmdManager.getCmd(messagePackage.getHeader().getMessageId());
    }

    /** 获取 Seq */
    public int getSeq() {
        return messagePackage.getHeader().getSeq();
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
    public static MessageContext of(GameMessagePackage gameMessagePackage, IoSession session) {
        MessageContext context = new MessageContext();
        context.messagePackage = gameMessagePackage;
        context.session = session;
        return context;
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        GameMessageHeader header = messagePackage.getHeader();
        json.put("playerId", header.getPlayerId());
        json.put("messageId", header.getMessageId());
        json.put("messageType", header.getMessageType());
        json.put("seq", header.getSeq());
        json.put("version", header.getVersion());
        json.put("errorCode", header.getErrorCode());
        if (session != null) {
            // json.put("session", session.session().toString());
            json.put("session", session.channel().toString());
        }

        ICmd cmd = getCmd();
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

        // 客户端发送时间
        long begin = header.getSendTime();
        json.put("cost", System.currentTimeMillis() - begin);
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
