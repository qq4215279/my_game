package com.mumu.game.core.net.channel;

import com.mumu.game.core.net.coder.JProtobufDecoder;
import com.mumu.game.core.net.coder.WebSocketDecoder;
import com.mumu.game.core.net.coder.WebSocketJProtobufEncoder;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.handler.DispatchGameMessageHandler;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * WebSocketChannelInitializer
 * websocket ChannelInitializer
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:08
 */
public class WebSocketChannelInitializer extends ChannelInitializer {

    /** 消息处理器 */
    private final DispatchGameMessageHandler messageHandler;

    public WebSocketChannelInitializer(MessageHandlerListener listener, ServerInfo serverInfo) {
        this.messageHandler = new DispatchGameMessageHandler(listener, serverInfo);
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        // HttpServerCodec：将请求和应答消息解码为HTTP消息
        pipeline.addLast(new LoggingHandler());
        pipeline.addLast(new HttpServerCodec());
        // 将HTTP消息的多个部分合成一条完整的HTTP消息，支持参数对象解析，比如POST参数，设置聚合内容的最大长度
        pipeline.addLast(new HttpObjectAggregator(NetConstants.WEBSOCKET_AGGREGATOR_LEN));
        // 支持大数据流写入
        pipeline.addLast(new ChunkedWriteHandler());
        // 支持 WebSocket 数据压缩
        pipeline.addLast(new WebSocketServerCompressionHandler());
        // WebSocket 服务端处理
        pipeline.addLast(new WebSocketServerProtocolHandler(NetConstants.WEBSOCKET_URI, null, true, 0xffff));
        // WebSocket 客户端握手处理
        // pipeline.addLast(
        // new WebSocketClientProtocolHandler(
        // WebSocketClientHandshakerFactory.newHandshaker(
        // new URI("ws://127.0.0.1:7500/ws"), WebSocketVersion.V13, null, false, new
        // DefaultHttpHeaders())));
        // 解码器：WebSocket 协议窗解码为 byte[]
        pipeline.addLast(WebSocketDecoder.getInstance());
        // 解码器：byte[] 解码为消息类
        pipeline.addLast(JProtobufDecoder.getInstance());
        // 编码器：jprotobuf 编码为 byte[]
        pipeline.addLast(WebSocketJProtobufEncoder.getInstance());
        // 处理空余时间 默认客户端读超时时间：90s
        // int readerIdleTimeSeconds = NetConstants.READER_IDLE_TIME_SECONDS;
        pipeline.addLast(new IdleStateHandler(90, 0, 0));
        // 处理器
        pipeline.addLast(messageHandler);
        // 黑名单
        // pipeline.addLast(new RuleBasedIpFilter(new IpFilterRuleHandler(service.getBlackList())));
    }
}