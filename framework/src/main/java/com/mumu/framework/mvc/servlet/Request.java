package com.mumu.framework.mvc.servlet;

import com.mumu.framework.mvc.message.GameMessagePackage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Request
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:05
 */
public class Request<RequestMessage> {
    /** 请求参数 */
    private Object[] args;
    /** 参数map */
    private RequestMessage requestMessage = null;
    /** 是否已经解析过 */
    private volatile boolean parse = false;
    /** 创建时间 */
    private long createTime;

    /** Channel */
    private Channel channel;
    /** ChannelHandlerContext */
    private ChannelHandlerContext ctx;
    /**  */
    private GameMessagePackage gameMessagePackage;

    public Request(Channel channel, ChannelHandlerContext ctx, GameMessagePackage gameMessagePackage) {
        this.createTime = System.currentTimeMillis();

        this.channel = channel;
        this.ctx = ctx;
        this.gameMessagePackage = gameMessagePackage;
    }


    public RequestMessage getRequestMessage() {
        parseParam();
        return this.requestMessage;
    }

    private void parseParam() {
        if (parse) {
            return;
        }

        try {
            // TODO
//            RequestUtil.parseParamWithoutDecode(new String(bytes), paramMap);
            this.requestMessage = null;
        } catch (Exception e) {
            // Ingore
        }
        parse = true;
    }

    /**
     * 获取请求IP地址
     * @return
     */
    public String getIp() {
        return getRemoteAddress().getAddress().getHostAddress();
    }

    /**
     * 获取请求地址
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }
}
