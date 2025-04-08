/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.server;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * ReconnectServerMsgEA
 *
 * @author liuzhen
 * @version 1.0.0 2025/4/6 15:32
 */
@ProtobufClass
@Data
public class ReconnectServerMsgEA {
    private ClientServerBean clientServerBean;
}
