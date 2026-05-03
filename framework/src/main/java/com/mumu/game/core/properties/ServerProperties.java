package com.mumu.game.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mumu.game.core.net.consts.ServerProtocol;

import lombok.Data;

/**
 * ServerProperties
 * Netty服务端配置
 * @author liuzhen
 * @version 1.0.0 2026/5/2 21:46
 */
@Data
@ConfigurationProperties(prefix = "net.server")
public class ServerProperties {
    /** 是否开启 */
    private boolean enable;
    /** 协议类型（SOCKET/HTTP/WEBSOCKET） */
    private ServerProtocol protocol = ServerProtocol.SOCKET;
    /** 服务端ip */
    private String ip = "127.0.0.1";
    /** 服务端口 */
    private int port;
    /** 是否是主服务（即根据server路由时，优先选择master服） */
    private boolean master;
    /** boss组线程数 */
    private int bossThreadNum = 1;
    /** worker组线程数 */
    private int workThreadNum = 64;
    /** 最大等待连接数量 */
    private int soBacklog = 128;
    /** tpc keepalive */
    private boolean soKeepalive;
    /** 是否开启 nagle算法（即在发送数据时将小的、碎片化的数据拼接成大报文发送，提高效率，但会增大数据延时） */
    private boolean tcpNoDelay;
    /** 接收缓冲区的大小 512kb */
    private int receiveSize = 512 * 1024;
    /** 发送缓冲区的大小 512kb */
    private int sendSize = 512 * 1024;
}
