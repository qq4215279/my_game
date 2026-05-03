package com.mumu.game.core.net.coder;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * WebSocketDecoder
 * websocket 解码器
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:20
 */
@ChannelHandler.Sharable
public class WebSocketDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> list)
            throws Exception {
        if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            ByteBuf content = binaryFrame.content();

            byte[] by = new byte[content.readableBytes()];
            content.readBytes(by);

            list.add(Unpooled.wrappedBuffer(by));
            // 释放前 fix：LEAK: ByteBuf.release() was not called before it's garbage-collected
            // 释放后 fix：io.netty.handler.codec.DecoderException: io.netty.util.IllegalReferenceCountException: refCnt: 0, decrement: 1
            // ReferenceCountUtil.release(frame);
        }
    }

    public static WebSocketDecoder getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final WebSocketDecoder INSTANCE = new WebSocketDecoder();
    }
}
