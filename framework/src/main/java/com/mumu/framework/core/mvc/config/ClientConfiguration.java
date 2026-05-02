package com.mumu.framework.core.mvc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.cloud.ServerInfo;
import com.mumu.framework.core.mvc.constants.NetConstants;
import com.mumu.framework.core.mvc.message.MessageHandlerListener;

import cn.hutool.core.thread.NamedThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;

/**
 * 注册客户端组件
 * @author liuzhen
 * @version 1.0.0 2025/6/15 16:58
 */
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
@ConditionalOnProperty(prefix = "net.client", name = "enable", havingValue = "true")
public class ClientConfiguration {

  /** 本服信息 */
  private final ServerInfo serverInfo;

  /** 客户端配置 */
  private final ClientProperties clientProperties;

  private EventLoopGroup clientGroup;

  public ClientConfiguration(ServerInfo serverInfo, ClientProperties clientProperties) {
    this.serverInfo = serverInfo;
    this.clientProperties = clientProperties;
  }

  /** 配置 netty client 线程组 */
  @Bean
  public EventLoopGroup clientGroup() {
    return clientGroup =
        new NioEventLoopGroup(
            clientProperties.getClientThreadNum(),
            new NamedThreadFactory(NetConstants.LOOP_GROUP_CLIENT_WORK, false));
  }

  @Bean
  public Bootstrap nettyClient(@Autowired(required = false) MessageHandlerListener listener) {
    listener = listener == null ? MessageHandlerListener.DUMMY : listener;
    listener.onStart();
    LogTopic.NET.info(
        "nettyClient",
        "group",
        serverInfo.getGroup(),
        "serverName",
        serverInfo.getServerName(),
        "serverId",
        serverInfo.getId(),
        "listener",
        listener.getClass().getName(),
        "clientProperties",
        clientProperties);

    Bootstrap b = new Bootstrap();
    b.group(clientGroup())
        .channel(NioSocketChannel.class)
        .handler(clientProperties.getProtocol().getChannelInitializer(listener, serverInfo))
        .option(ChannelOption.SO_KEEPALIVE, clientProperties.isSoKeepalive())
        .option(ChannelOption.TCP_NODELAY, clientProperties.isTcpNoDelay())
        .option(ChannelOption.SO_RCVBUF, clientProperties.getReceiveSize())
        .option(ChannelOption.SO_SNDBUF, clientProperties.getSendSize());
    return b;
  }

  @PreDestroy
  public void destroy() {
    LogTopic.NET.info("容器关闭 nettyClient stop...");
    if (clientGroup != null) clientGroup.shutdownGracefully();
    LogTopic.NET.info(
        "容器关闭 nettyClient stop finish",
        "group",
        serverInfo.getGroup(),
        "serverName",
        serverInfo.getServerName(),
        "serverId",
        serverInfo.getId());
  }
}
