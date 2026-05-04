package com.mumu.game.core.net.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.core.properties.ServerProperties;

import cn.hutool.core.thread.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * NettyServerConfiguration 注册netty服务端配置
 * 
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:57
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
@ConditionalOnProperty(prefix = "net.server", name = "enable", havingValue = "true")
public class NettyServerConfiguration {
    /** 本服信息 */
    private final ServerInfo serverInfo;

    /** 服务端配置 */
    private final ServerProperties serverProperties;

    public NettyServerConfiguration(ServerInfo serverInfo, ServerProperties serverProperties) {
        this.serverInfo = serverInfo;
        this.serverProperties = serverProperties;
    }

    /** 配置 netty boss 线程组 */
    @Bean
    public EventLoopGroup bossGroup() {
        return new NioEventLoopGroup(serverProperties.getBossThreadNum(),
            new NamedThreadFactory(NetConstants.LOOP_GROUP_SERVER_BOSS, false));
    }

    /** 配置 netty work 线程组 */
    @Bean
    public EventLoopGroup workGroup() {
        return new NioEventLoopGroup(serverProperties.getWorkThreadNum(),
            new NamedThreadFactory(NetConstants.LOOP_GROUP_SERVER_WORK, false));
    }

    @Bean
    public ServerBootstrap nettyServer(@Autowired(required = false) MessageHandlerListener listener) {
        listener = listener == null ? MessageHandlerListener.DUMMY : listener;
        listener.handleStart();
        LogTopic.NET.info("nettyServer", "serviceType", serverInfo.getServiceType(), "serverName",
            serverInfo.getServerName(), "serverId", serverInfo.getServerId(), "listener", listener.getClass().getName(),
            "serverProperties", serverProperties);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup(), workGroup()).channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, serverProperties.getSoBacklog())
            // .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(serverProperties.getProtocol().getChannelInitializer(listener, serverInfo))
            .childOption(ChannelOption.SO_KEEPALIVE, serverProperties.isSoKeepalive())
            .childOption(ChannelOption.TCP_NODELAY, serverProperties.isTcpNoDelay())
            .childOption(ChannelOption.SO_RCVBUF, serverProperties.getReceiveSize())
            .childOption(ChannelOption.SO_SNDBUF, serverProperties.getSendSize());
        return serverBootstrap;
    }
}
