package com.mumu.framework.util;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * NettyUtil
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 22:50
 */
public class NettyUtil {

    /**
     * getRemoteIP
     */
    public static String getRemoteIP(Channel channel) {
        InetSocketAddress ipSocket = (InetSocketAddress)channel.remoteAddress();
        return ipSocket.getAddress().getHostAddress();
    }
}
