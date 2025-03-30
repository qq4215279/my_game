///*
// * Copyright 2020-2025, mumu without 996.
// * All Right Reserved.
// */
//
//package com.mumu.gateway.net.config;
//
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//
///**
// * ClientProperties
// * Netty客户端配置
// *
// * @author liuzhen
// * @version 1.0.0 2025/3/24 22:30
// */
//@Data
//@ConfigurationProperties(prefix = "game.client")
//public class ClientProperties {
//    /** 是否开启 */
//    private boolean enable;
//
////    /** 协议类型（SOCKET/HTTP/WEBSOCKET） */
////    private ServerProtocol protocol = ServerProtocol.SOCKET;
//
//    /** worker组线程数 */
//    private int clientThreadNum = 64;
//
//    /** tpc keepalive */
//    private boolean soKeepalive;
//
//    /** 是否开启 nagle算法（即在发送数据时将小的、碎片化的数据拼接成大报文发送，提高效率，但会增大数据延时） */
//    private boolean tcpNoDelay;
//
//    /** 接收缓冲区的大小 512kb */
//    private int receiveSize = 512 * 1024;
//
//    /** 发送缓冲区的大小 512kb */
//    private int sendSize = 512 * 1024;
//}
