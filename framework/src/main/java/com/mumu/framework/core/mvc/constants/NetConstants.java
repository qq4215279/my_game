/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.constants;

import com.mumu.framework.core.mvc.cloud.ServerInfo;

import io.netty.util.AttributeKey;

/**
 * NetConstants
 * 网络工具相关常量
 * @author liuzhen
 * @version 1.0.0 2025/3/24 22:44
 */
public interface NetConstants {
  /** 客户端标识（主动注册标识） */
  AttributeKey<Boolean> SESSION_CLIENT = AttributeKey.valueOf("connect-client");
  /** 连接所属服务类型 - ServerType */
  AttributeKey<ServiceType> SESSION_SERVICE_TYPE = AttributeKey.valueOf("connect-service-type");
  /** 连接所属服务ID - serverId */
  AttributeKey<Integer> SESSION_SERVER_ID = AttributeKey.valueOf("connect-server-id");
  /** 服务器信息 - ServerInfo */
  AttributeKey<ServerInfo> SESSION_SERVER_INFO = AttributeKey.valueOf("connect-server-info");

  /** 玩家ID标识 - playerId */
  AttributeKey<Long> SESSION_PLAYER_ID = AttributeKey.valueOf("connect-player-id");



  /** 玩家离线标识 true表示已经处理过下线 */
  AttributeKey<Boolean> SESSION_PLAYER_EXIT_FLAG = AttributeKey.valueOf("connect-player-exit-flag");

  /** session关闭原因 */
  // AttributeKey<ExitReason> SESSION_CLOSE_REASON = AttributeKey.valueOf("connect-close-reason");

  /** 客户端IP地址 */
  AttributeKey<String> SESSION_CLIENT_IP = AttributeKey.valueOf("client_ip");

  /** 连接所属服务类型 */
  String SESSION_ATTR_GROUP = "connect-server-group";

  /** 连接所属服务ID */
  String SESSION_ATTR_ID = "connect-server-id";

  /** 服务注册 */
  String SESSION_ATTR_REGISTER = "connect-register";

  String SESSION_ATTR_CONNECT_NAME = "connect-name";
  String SESSION_ATTR_CONNECT_ID = "connect-id";
  String SESSION_ATTR_CONNECT_IP = "connect-ip";
  String SESSION_ATTR_CONNECT_PORT = "connect-port";
  String SESSION_ATTR_CONNECT_TYPE = "connect-type";
  // String SESSION_ATTR_CONNECT_CLUSTER = "connect-server-cluster";
  // String SESSION_ATTR_CONNECT_ROLE = "connect-server-role";
  // String SESSION_ATTR_CONNECT_OPENING = "connect-server-opening";
  // String SESSION_ATTR_CONNECT_CLOSED = "connect-server-closed";
  // String SESSION_ATTR_LAST_LIVE_TIME = "connect-server-last-live-time";

  String LOOP_GROUP_SERVER_BOSS = "netty-server-boss-";
  String LOOP_GROUP_SERVER_WORK = "netty-server-work-";
  String LOOP_GROUP_CLIENT_WORK = "netty-client-work-";

  /** WebSocket http集合包大小 */
  int WEBSOCKET_AGGREGATOR_LEN = 65535;

  /** WebSocket 暴露的 uri地址 */
  String WEBSOCKET_URI = "/ws";

  /** 读请求空闲时间 */
  int READER_IDLE_TIME_SECONDS = 90;
}
