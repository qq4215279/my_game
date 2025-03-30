/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.system.message;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * GameMessagePackage
 * 游戏消息包
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:30
 */
@ProtobufClass
@Data
public class GameMessagePackage {
    /** 头信息 */
    private GameMessageHeader header;
    /** 包体数据 */
    private byte[] body;
}
