package com.mumu.game.core.mvc.servlet;

import java.io.IOException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;

/**
 * Response
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:05
 */
@Getter
public class Response {
    /** channel */
    private Channel channel;
    /** 是否关闭 */
    private boolean close = false;

    public Response(Channel channel) {
        this.channel = channel;
    }

    public boolean isWritable() {
        return channel.isWritable();
    }

    public ChannelFuture write(Object obj) throws IOException {
        ChannelFuture future = channel.writeAndFlush(obj);
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
        return null;
    }

    public void markClose() {
        this.close = true;
    }
}
