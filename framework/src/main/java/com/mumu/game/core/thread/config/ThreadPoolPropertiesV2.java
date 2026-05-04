// package com.mumu.game.core.thread.config;
//
// import org.springframework.boot.context.properties.ConfigurationProperties;
//
// import lombok.Data;
//
// /**
//  * ThreadPoolProperties
//  * 线程池配置（工作线程池 和 玩家线程池）
//  * @author liuzhen
//  * @version 1.0.0 2025/3/16 15:52
//  */
// @Data
// @ConfigurationProperties(prefix = "game.thread")
// @Deprecated
// public class ThreadPoolPropertiesV2 {
//     /** 默认线程数 cup数量*2 */
//     private static final int DEF_THREAD_NUM = Runtime.getRuntime().availableProcessors() << 1;
//
//     /** 是否开启 */
//     private boolean enable;
//     /** 玩家核心线程数 */
//     private int playerCorePoolSize = 64;
//     /** 玩家每线程队列大小 */
//     private int playerMaxQueueSize = 10000;
//     /** 服务器核心线程数 */
//     private int serverCorePoolSize = DEF_THREAD_NUM;
//     /** 服务器最大线程数 */
//     private int serverMaxPoolSize = DEF_THREAD_NUM;
//     /** 服务器空闲线程存话时间（秒） */
//     private int serverKeepAliveTime = 30;
//     /** 服务器线程池队列大小 */
//     private int serverMaxQueueSize = 10000;
// }
