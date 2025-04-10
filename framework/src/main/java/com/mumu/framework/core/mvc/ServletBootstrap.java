/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc;

import com.google.common.util.concurrent.RateLimiter;
import com.mumu.framework.core.mvc.message.GatewayHandlerListener;
import com.mumu.framework.core.mvc.message.MessageHandlerListener;
import com.mumu.framework.core.mvc.servlet.initializer.GatewayServletChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * ServletBootstrap
 * Servlet启动器
 * @author liuzhen
 * @version 1.0.0 2025/2/24 22:53
 */
@Service
public class ServletBootstrap {
    @Resource
    private GatewayServerConfig serverConfig;


    private RateLimiter globalRateLimiter;
    private NioEventLoopGroup bossGroup = null;
    private NioEventLoopGroup workerGroup = null;

    /**
     * 启动
     */
    public void start() {
        startTcpAndHttp();
    }

    private void startTcpAndHttp() {
        globalRateLimiter = RateLimiter.create(serverConfig.getGlobalRequestPerSecond());

        // boss逻辑线程组
        bossGroup = new NioEventLoopGroup(serverConfig.getBossThreadCount());
        // 业务逻辑线程组
        workerGroup = new NioEventLoopGroup(serverConfig.getWorkThreadCount());

        int port = this.serverConfig.getPort();
        try {
            // TODO 动态获取
            MessageHandlerListener messageHandlerListener = new GatewayHandlerListener();

            ServerBootstrap b = new ServerBootstrap();
            // 这里遇到一个小问题，如果把childHandler的加入放在option的前面，option将会不生效。我用java socket连接，一直没有消息返回。
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new GatewayServletChannelInitializer(messageHandlerListener, serverConfig,
                            globalRateLimiter));

//            logger.info("开始启动服务，端口:{}", serverConfig.getPort());

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

}
