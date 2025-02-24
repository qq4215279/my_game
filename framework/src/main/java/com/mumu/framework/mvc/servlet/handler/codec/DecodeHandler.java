package com.mumu.framework.mvc.servlet.handler.codec;

import com.mumu.framework.mvc.message.GameMessageHeader;
import com.mumu.framework.mvc.message.GameMessagePackage;
import com.mumu.framework.util.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;

/**
 * DecodeHandler
 * 消息解码handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:32
 */
public class DecodeHandler extends ChannelInboundHandlerAdapter {
    /** 对称加密密钥 */
    @Setter
    private String aesSecret;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
//        try {
//            int messageSize = byteBuf.readInt();
//            int clientSeqId = byteBuf.readInt();
//            int messageId = byteBuf.readInt();
//            int serviceId = byteBuf.readShort();
//            long clientSendTime = byteBuf.readLong();
//            int version = byteBuf.readInt();
//            int compress = byteBuf.readByte();
//            byte[] body = null;
//            if (byteBuf.readableBytes() > 0) {
//                body = new byte[byteBuf.readableBytes()];
//                byteBuf.readBytes(body);
//
//                // 如果密钥不为空，且不是认证消息，对消息体解密
//                if (this.aesSecret != null && messageId != 1) {
//                    body = AESUtils.decode(aesSecret, body);
//                }
//                if (compress == 1) {
//                    body = CompressUtil.decompress(body);
//                }
//            }
//
//            GameMessageHeader header = new GameMessageHeader();
//            header.setClientSendTime(clientSendTime);
//            header.setClientSeqId(clientSeqId);
//            header.setMessageId(messageId);
//            header.setServiceId(serviceId);
//            header.setMessageSize(messageSize);
//            header.setVersion(version);
//            GameMessagePackage gameMessagePackage = new GameMessagePackage();
//            gameMessagePackage.setHeader(header);
//            gameMessagePackage.setBody(body);
//
//            ctx.fireChannelRead(gameMessagePackage);
//
//        } finally {
//            ReferenceCountUtil.release(byteBuf);
//        }
    }
}
