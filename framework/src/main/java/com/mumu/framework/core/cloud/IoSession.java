/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cloud;

import java.net.InetSocketAddress;

import com.mumu.framework.core.mvc.servlet.constants.NetConstants;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * IoSession
 * 原始的连接对象，如 netty的Channel，mina的IoSession等
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/28 23:14
 */
public record IoSession(Channel channel) {

    /** 获取连接的唯一key */
    public String getKey() {
        return channel().id().toString();
    }

    /** 判断连接上是否存在指定 key 的属性 */
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return channel().hasAttr(key);
    }

    /** 获取连接上指定 key 的属性 */
    public <T> T getAttr(AttributeKey<T> key) {
        return getAttr(key, null);
    }

    /** 获取连接上指定 key 的属性，没有则返回默认值 */
    public <T> T getAttr(AttributeKey<T> key, T orElse) {
        return hasAttr(key) ? channel().attr(key).get() : orElse;
    }

    /** 设置连接上指定 key 的属性 */
    public <T> void setAttr(AttributeKey<T> key, T field) {
        channel().attr(key).set(field);
    }

    /** 关闭连接 */
    public void close() {
        channel().close();
    }

    /** 发送数据 */
    public void write(Object msg) {
        channel().writeAndFlush(msg);
    }

    /** 获取远程用户地址 */
    public String getClientIp() {
        return getAttr(
                NetConstants.SESSION_CLIENT_IP,
                ((InetSocketAddress) channel().remoteAddress()).getAddress().getHostAddress());
    }

    // TODO
    public static String getRemoteIP(Channel channel) {
        InetSocketAddress ipSocket = (InetSocketAddress)channel.remoteAddress();
        String remoteHost = ipSocket.getAddress().getHostAddress();
        return remoteHost;
    }

    public static IoSession of(Channel session) {
        return new IoSession(session);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof IoSession && channel().equals(((IoSession) obj).channel()));
    }

}
