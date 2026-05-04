package com.mumu.game.core.net.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.NetConstants;
import com.mumu.game.core.net.listener.MessageHandlerListener;
import com.mumu.game.core.properties.ClientProperties;
import com.mumu.game.core.properties.ServerInfo;

import cn.hutool.core.thread.NamedThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;

/**
 * NettyClientConfiguration
 * 注册netty客户端组件
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:49
 */
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
// 这个注解组合起来表示：只有当配置文件（如 application.yml）中存在 net.server.enable=true 时，才会加载被注解的 Bean 或配置类。
@ConditionalOnProperty(prefix = "net.client", name = "enable", havingValue = "true")
public class NettyClientConfiguration {
    /** 本服信息 */
    private final ServerInfo serverInfo;

    /** 客户端配置 */
    private final ClientProperties clientProperties;

    private EventLoopGroup clientGroup;

    public NettyClientConfiguration(ServerInfo serverInfo, ClientProperties clientProperties) {
        this.serverInfo = serverInfo;
        this.clientProperties = clientProperties;
    }

    /** 配置 netty client 线程组 */
    @Bean
    public EventLoopGroup clientGroup() {
        return clientGroup = new NioEventLoopGroup(clientProperties.getClientThreadNum(),
            new NamedThreadFactory(NetConstants.LOOP_GROUP_CLIENT_WORK, false));
    }

    @Bean
    public Bootstrap nettyClient(@Autowired(required = false) MessageHandlerListener listener) {
        listener = listener == null ? MessageHandlerListener.DUMMY : listener;
        listener.handleStart();
        LogTopic.NET.info("nettyClient", "serviceType", serverInfo.getServiceType(), "serverName",
            serverInfo.getServerName(), "serverId", serverInfo.getServerId(), "listener", listener.getClass().getName(),
            "clientProperties", clientProperties);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup()).channel(NioSocketChannel.class)
            .handler(clientProperties.getProtocol().getChannelInitializer(listener, serverInfo))
            .option(ChannelOption.SO_KEEPALIVE, clientProperties.isSoKeepalive())
            .option(ChannelOption.TCP_NODELAY, clientProperties.isTcpNoDelay())
            .option(ChannelOption.SO_RCVBUF, clientProperties.getReceiveSize())
            .option(ChannelOption.SO_SNDBUF, clientProperties.getSendSize());
        return bootstrap;
    }

    @PreDestroy
    public void destroy() {
        LogTopic.NET.info("容器关闭 nettyClient stop...");
        if (clientGroup != null) {
            clientGroup.shutdownGracefully();
        }
        LogTopic.NET.info("容器关闭 nettyClient stop finish", "serviceType", serverInfo.getServiceType(), "serverName",
            serverInfo.getServerName(), "serverId", serverInfo.getServerId());
    }
}
