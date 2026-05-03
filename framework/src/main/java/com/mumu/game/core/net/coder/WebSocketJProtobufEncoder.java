package com.mumu.game.core.net.coder;

import java.util.List;

import com.mumu.game.core.utils.JProtoBufUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * WebSocketJProtobufEncoder
 * WebSocket 的 JProtobuf 编码器
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:23
 */
@ChannelHandler.Sharable
public class WebSocketJProtobufEncoder extends MessageToMessageEncoder<MessageProxy> {

    @Override
    protected void encode(
            ChannelHandlerContext channelHandlerContext, MessageProxy proxy, List<Object> out)
            throws Exception {
        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(JProtoBufUtil.encode(proxy))));
    }

    public static WebSocketJProtobufEncoder getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final WebSocketJProtobufEncoder INSTANCE = new WebSocketJProtobufEncoder();
    }
}