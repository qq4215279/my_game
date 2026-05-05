package com.mumu.game.core.net.server;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.core.properties.ServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * AutoServerStarter 服务端组件 开始/停止监听端口
 * @author liuzhen
 * @version 1.0.0 2026/5/4 23:13
 */
@Component
@ConditionalOnBean(ServerBootstrap.class)
public class AutoServerStarter implements SmartLifecycle {

    /**
     * {@link SmartLifecycle} 启动阶段，值越小越先执行 {@link #start()}（绑定端口）
     * 其它需在 Netty 监听就绪后执行的组件应使用大于本常量的 phase
     */
    public static final int LIFECYCLE_PHASE = 0;

    /** 本服信息 */
    @Resource
    private ServerInfo serverInfo;
    /** 注册服配置 */
    @Resource
    private ServerProperties serverProperties;

    /** 本服服务端 */
    @Resource
    private ServerBootstrap nettyServer;

    /** 服务端 bossGroup */
    @Resource
    private EventLoopGroup bossGroup;
    /** 服务端 workGroup */
    @Resource
    private EventLoopGroup workGroup;

    /** 组件的运行状态 */
    private volatile boolean running = false;

    private ChannelFuture future;

    @Override
    public void start() {
        try {
            future = nettyServer.bind(serverProperties.getPort()).sync();
            running = true;
            LogTopic.NET.info("nettyServerStart", "netty启动成功~", "serviceType", serverInfo.getServiceType(),
                    "serverName",
                serverInfo.getServerName(), "serverId", serverInfo.getServerId(), "port", serverProperties.getPort());
        } catch (InterruptedException e) {
            LogTopic.NET.error(e, "nettyServerStart", "netty启动失败~", "serviceType", serverInfo.getServiceType(),
                "serverName", serverInfo.getServerName(), "serverId", serverInfo.getServerId(), "port",
                serverProperties.getPort());
        }
    }

    @Override
    public void stop() {
        LogTopic.NET.info("容器关闭 nettyServer stop...");
        if (future != null) {
            future.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().awaitUninterruptibly();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully().awaitUninterruptibly();
        }
        LogTopic.NET.info("容器关闭 nettyServer stop finish", "serviceType", serverInfo.getServiceType(), "serverName",
            serverInfo.getServerName(), "serverId", serverInfo.getServerId(), "port", serverProperties.getPort());
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LIFECYCLE_PHASE;
    }
}