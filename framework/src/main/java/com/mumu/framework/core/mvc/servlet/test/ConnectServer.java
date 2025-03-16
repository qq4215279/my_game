package com.mumu.framework.core.mvc.servlet.test;

import cn.hutool.core.util.RandomUtil;
import com.mumu.common.mvc.cloud.ServerInfo;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.thread.ScheduledExecutorUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import jakarta.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import org.jctools.maps.NonBlockingHashSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * ConnectServer
 * 客户端连接器
 * @author liuzhen
 * @version 1.0.0 2025/3/16 16:26
 */
@Component
@ConditionalOnBean(Bootstrap.class)
public class ConnectServer {
  /** 尝试重连间隔（秒） */
  private static final int RETRY_INTERVAL = 5;

  /** 最大重连次数 0：无限重连 */
  private static final int MAX_RETRY_COUNT = 10;

  /** 本服信息 */
  @Resource
  private ServerInfo serverInfo;

  /** 本服客户端（只有注册了客户端才会获取到） */
  @Resource private Bootstrap nettyClient;

  /** 正在连接中的请求缓存 */
  private final NonBlockingHashSet<ConnectionRequest> connectingMap = new NonBlockingHashSet<>();

  /** 连接指定服务器 */
  public void connect(ServerGroup group, String ip, int port) {
    connect(group, ip, port, RETRY_INTERVAL, MAX_RETRY_COUNT);
  }

  /** 连接指定服务器 */
  public void connect(ServerGroup group, String ip, int port, int retryInterval, int retryCount) {
    ConnectionRequest request = new ConnectionRequest(group, ip, port);
    boolean contains = connectingMap.contains(request);
    LogTopic.NET.info(
        "ConnectServer.connect",
        // "group",
        // serverInfo.getGroup(),
        // "serverName",
        // serverInfo.getServerName(),
        // "serverId",
        // serverInfo.getId(),
        "connectGroup",
        group,
        "connectIp",
        ip,
        "connectPort",
        port,
        "contains",
        contains);

    if (connectingMap.contains(request)) {
      return;
    }
    request.setRetryInterval(retryInterval);
    request.setMaxRetryCount(retryCount);
    connectingMap.add(request);
    ScheduledExecutorUtil.schedule(request, request.calRetryInterval(), TimeUnit.SECONDS);
  }

  @Getter
  private class ConnectionRequest implements Runnable {
    /** 服务组 */
    private final ServerGroup group;

    /** ip */
    private final String ip;

    /** 端口 */
    private final int port;

    /** 尝试重连间隔（秒） */
    @Setter
    private int retryInterval = RETRY_INTERVAL;

    /** 最大重连次数 0：无限重连 */
    @Setter private int maxRetryCount = MAX_RETRY_COUNT;

    /** 已尝试重连次数 */
    private int retryCount;

    /** 是否成功 */
    private volatile boolean successed;

    public ConnectionRequest(ServerGroup group, String ip, int port) {
      this.group = group;
      this.ip = ip;
      this.port = port;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ConnectionRequest request = (ConnectionRequest) o;
      return port == request.port && group == request.group && Objects.equals(ip, request.ip);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, ip, port);
    }

    /** 计算下次重连等待时间 */
    private long calRetryInterval() {
      return (long) retryCount * retryInterval + RandomUtil.randomInt(0, retryInterval);
    }

    @Override
    public void run() {
      nettyClient
          .connect(ip, port)
          .addListener(
              (ChannelFutureListener)
                  future -> {
                    if (future.isSuccess()) {
                      success(future);
                    } else {
                      retry();
                    }
                  });
      // connect.awaitUninterruptibly(retryInterval, TimeUnit.SECONDS);
      // if (connect.isSuccess()) success(connect);
      // else retry();
    }

    /** 连接成功 */
    private void success(ChannelFuture connect) {
      successed = true;
      // 设置基本属性
      Channel channel = connect.channel();
      channel.attr(NetConstants.SESSION_CLIENT).set(true);
      channel.attr(NetConstants.SESSION_SERVER_GROUP).set(group);
      LogTopic.NET.info(
          "ConnectServer success",
          // "group",
          // serverInfo.getGroup(),
          // "serverName",
          // serverInfo.getServerName(),
          // "serverId",
          // serverInfo.getId(),
          "connectGroup",
          group,
          "connectIp",
          ip,
          "connectPort",
          port);
      // 发送本服信息进行握手
      // MessageSender.send(IoSession.of(channel), Cmd.ReqServerInfoHandshake, serverInfo.build());
      connectingMap.remove(this);
    }

    /** 失败重试 */
    private void retry() {
      ++retryCount;
      long interval = calRetryInterval();
      LogTopic.NET.warn(
          "ConnectServer fail retry",
          // "group",
          // serverInfo.getGroup(),
          // "serverName",
          // serverInfo.getServerName(),
          // "serverId",
          // serverInfo.getId(),
          "connectGroup",
          group,
          "connectIp",
          ip,
          "connectPort",
          port,
          "interval",
          interval,
          "retryCount",
          retryCount,
          "maxRetryCount",
          maxRetryCount);
      if (maxRetryCount == 0 || retryCount < maxRetryCount) {
        ScheduledExecutorUtil.schedule(this, interval, TimeUnit.SECONDS);
      }
    }
  }
}
