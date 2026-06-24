/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.proto.message.system.message;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import com.mumu.game.proto.message.core.ErrorCode;
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

    public Long getPlayerId() {
        return header.getPlayerId();
    }

    public int getSeq() {
        return 100;
    }

    public ErrorCode getErrorCode() {
        return header.getErrorCode();
    }
}
