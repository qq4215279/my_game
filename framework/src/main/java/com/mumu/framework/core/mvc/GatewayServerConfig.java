package com.mumu.framework.core.mvc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GatewayServerConfig
 *
 * @author liuzhen
 * @version 1.0.0 2025/2/24 22:58
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "game.gateway.server.config")
public class GatewayServerConfig {
    /** 服务器ID */
    private int serverId;
    /** 端口号 */
    private int port;
    /** boss线程数 */
    private int bossThreadCount;
    /** work线程数 */
    private int workThreadCount;
    /** work线程数 */
    private long recBufSize;
    /** work线程数 */
    private long sendBufSize;
    /** 达到压缩的消息最小大小 */
    private int compressMessageSize = 1024 * 2;
    /** 等待认证的超时时间 */
    private int waiteConfirmTimeoutSecond = 30;
    /** 单个用户的限流请允许的每秒请求数量 */
    private double requestPerSecond = 10;
    /** 全局流量限制请允许每秒请求数量 */
    private double globalRequestPerSecond = 2000;
    /** channel读取空闲时间 */
    private int readerIdleTimeSeconds = 300;
    /** channel写出空闲时间 */
    private int writerIdleTimeSeconds = 12;
    /** 读写空闲时间 */
    private int allIdleTimeSeconds = 15;
}
