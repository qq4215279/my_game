/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler.codec;

import java.util.List;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.util.JProtoBufUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.Setter;

/**
 * JProtobufEncoder
 * JProtobuf编码器
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:19
 */
public class JProtobufEncoder extends MessageToMessageEncoder<GameMessagePackage> {
    /** 对称加密密钥 */
    @Setter
    private String aesSecret;

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, GameMessagePackage proxy, List<Object> out) {
        out.add(Unpooled.wrappedBuffer(JProtoBufUtil.encode(proxy)));
    }
}
