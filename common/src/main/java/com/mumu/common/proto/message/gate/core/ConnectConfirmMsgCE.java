/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.gate.core;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * ConnectConfirmMsgCE
 * 连接验证消息请求
 * @author liuzhen
 * @version 1.0.0 2025/3/30 14:01
 */
@ProtobufClass
@Data
public class ConnectConfirmMsgCE {
    /** 加密token */
    private String token;
}
