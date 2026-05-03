package com.mumu.game.core.net.channel;

import java.util.concurrent.TimeUnit;

import com.mumu.game.core.net.coder.JProtobufDecoder;
import com.mumu.game.core.net.coder.JProtobufEncoder;
import com.mumu.game.core.net.handler.DispatchGameMessageHandler;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * SocketChannelInitializer
 * socket ChannelInitializer
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:08
 */
public class SocketChannelInitializer extends ChannelInitializer {

    private static final ProtobufVarint32LengthFieldPrepender PROTOBUF_LENGTH_PREPENDER =
            new ProtobufVarint32LengthFieldPrepender();

    /** 消息处理器 */
    private final DispatchGameMessageHandler messageHandler;

    public SocketChannelInitializer(MessageHandlerListener listener, ServerInfo serverInfo) {
        this.messageHandler = new DispatchGameMessageHandler(listener, serverInfo);
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline
                // 入参说明: 读超时时间、写超时时间、所有类型的超时时间、时间格式
                .addLast(new IdleStateHandler(200, 200, 400, TimeUnit.MILLISECONDS))
                // 解码器：解码 length 和 byte[]
                .addLast(new ProtobufVarint32FrameDecoder())
                // 解码器：byte[] 解码为消息类
                .addLast(JProtobufDecoder.getInstance())
                // 编码器：添加 length
                .addLast(PROTOBUF_LENGTH_PREPENDER)
                // 编码器：jprotobuf 编码为 byte[]
                .addLast(JProtobufEncoder.getInstance())
                .addLast(messageHandler);
    }
}
