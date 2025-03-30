/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.gate.core;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * ConnectConfirmMsgEC
 * 连接验证消息响应
 * @author liuzhen
 * @version 1.0.0 2025/3/30 14:01
 */
@ProtobufClass
@Data
public class ConnectConfirmMsgEC {
    /** 对称加密密钥，客户端需要使用非对称加密私钥解密才能获得。 */
    private String secretKey;
}
