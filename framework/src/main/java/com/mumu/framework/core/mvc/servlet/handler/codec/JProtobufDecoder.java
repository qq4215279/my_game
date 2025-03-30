/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler.codec;

import java.util.List;

import com.mumu.framework.util.JProtoBufUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.Setter;

/**
 * JProtobufDecoder
 * JProtobuf解码器
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:15
 */
@Setter
public class JProtobufDecoder extends MessageToMessageDecoder<ByteBuf> {
    /** 对称加密密钥 */
    private String aesSecret;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg, List<Object> out)
            throws Exception {
        int length = msg.readableBytes();
        byte[] array;
        int offset;
        if (msg.hasArray()) {
            array = msg.array();
            offset = msg.arrayOffset() + msg.readerIndex();
        } else {
            array = ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false);
            offset = 0;
        }
        out.add(JProtoBufUtil.decodeMessageProxy(array, offset, length));
    }
}