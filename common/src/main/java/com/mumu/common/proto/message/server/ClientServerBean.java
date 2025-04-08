/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.server;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;

/**
 * ClientServerBean
 * 服务器信息
 *
 * @author liuzhen
 * @version 1.0.0 2025/4/4 15:59
 */
@Data
@ProtobufClass
public class ClientServerBean {
    /** 服务器组类型 */
    private int serviceId;
    /** 服务器编号 */
    private int serverId;
    /** 服务器ip */
    private String ip;
    /** 服务器端口 */
    private Integer port;
}
