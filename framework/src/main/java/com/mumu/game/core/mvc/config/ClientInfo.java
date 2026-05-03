// /*
//  *
//  *  * Copyright 2020-2026, mumu without 996.
//  *  * All Right Reserved.
//  *
//  */
//
// package com.mumu.framework.core.mvc.config;
//
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;
//
// import com.mumu.framework.core.mvc.constants.ServerProtocol;
// import com.mumu.framework.core.mvc.constants.ServiceType;
//
// import lombok.Data;
//
// /**
//  * ClientInfo
//  * 客户端链接信息
//  * @author liuzhen
//  * @version 1.0.0 2025/6/15 17:04
//  */
// @Data
// @Component
// public class ClientInfo {
//
//     @Value("${application.name}")
//     private String serverName;
//
//     @Value("${net.id}")
//     private int id;
//
//     @Value("${net.group}")
//     private ServiceType group;
//
//     @Value("${net.server.enable:false}")
//     private boolean serverEnable;
//
//     @Value("${net.server.protocol:SOCKET}")
//     private ServerProtocol protocol;
//
//     @Value("${net.server.ip:127.0.0.1}")
//     private String ip;
//
//     @Value("${net.server.port:0}")
//     private int port;
//
//     @Value("${net.server.master:false}")
//     private boolean master;
//
//     /** 构造DTO数据 */
//     public ClientServerInfo build() {
//         ClientServerInfo info = new ClientServerInfo();
//         info.setServerId(id);
//         info.setServerName(serverName);
//         info.setGroup(group.name());
//         info.setServerEnable(serverEnable);
//         info.setProtocol(protocol.name());
//         info.setIp(ip);
//         info.setPort(port);
//         info.setMaster(master);
//         return info;
//     }
// }
