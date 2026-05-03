package com.mumu.game.core.net.coder;

import java.util.List;

import com.mumu.game.core.utils.JProtoBufUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * JProtobufEncoder
 * JProtobuf编码器
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:24
 */
@ChannelHandler.Sharable
public class JProtobufEncoder extends MessageToMessageEncoder<MessageProxy> {

    @Override
    protected void encode(
            ChannelHandlerContext channelHandlerContext, MessageProxy proxy, List<Object> out)
            throws Exception {
        out.add(Unpooled.wrappedBuffer(JProtoBufUtil.encode(proxy)));
    }

    public static JProtobufEncoder getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final JProtobufEncoder INSTANCE = new JProtobufEncoder();
    }
}
