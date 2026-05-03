package com.mumu.game.core.net.channel;

import com.mumu.game.core.net.handler.DispatchGameMessageHandler;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * HttpChannelInitializer
 * http ChannelInitializer
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:07
 */
public class HttpChannelInitializer extends ChannelInitializer {

    /** 统一的消息处理器 */
    private final MessageHandlerListener listener;
    /** 本服信息 */
    private final ServerInfo serverInfo;

    public HttpChannelInitializer(MessageHandlerListener listener, ServerInfo serverInfo) {
        this.listener = listener;
        this.serverInfo = serverInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(65535));
        pipeline.addLast(new HttpContentCompressor());
        pipeline.addLast(new DispatchGameMessageHandler(listener, serverInfo));
        // 黑名单
        // pipeline.addLast(new RuleBasedIpFilter(new IpFilterRuleHandler(service.getBlackList())));
    }
}

