package com.mumu.framework.core.mvc.servlet.handler.codec;

import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.message.GameMessagePackage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Setter;

/**
 * EncodeHandler
 * 消息编码handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:29
 */
public class EncodeHandler extends MessageToByteEncoder<GameMessagePackage> {
    private static final int GAME_MESSAGE_HEADER_LEN = 29;

    private GatewayServerConfig serverConfig;


    /** 对称加密密钥 */
    @Setter
    private String aesSecret;

    public EncodeHandler(GatewayServerConfig serverConfig) {
        // 注入服务端配置
        this.serverConfig = serverConfig;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, GameMessagePackage msg, ByteBuf out) throws Exception {
//        int messageSize = GAME_MESSAGE_HEADER_LEN;
//        byte[] body = msg.getBody();
//        int compress = 0;
//
//        if (body != null) {
//            // 达到压缩条件，进行压缩
//            if (body.length >= serverConfig.getCompressMessageSize()) {
//                body = CompressUtil.compress(body);
//                compress = 1;
//            }
//
//            if (this.aesSecret != null && msg.getHeader().getMessageId() != 1) {
//                body = AESUtils.encode(aesSecret, body);
//            }
//            messageSize += body.length;
//        }
//
//        out.writeInt(messageSize);
//
//        GameMessageHeader header = msg.getHeader();
//        out.writeInt(header.getClientSeqId());
//        out.writeInt(header.getMessageId());
//        out.writeLong(header.getServerSendTime());
//        out.writeInt(header.getVersion());
//        out.writeByte(compress);
//        out.writeInt(header.getErrorCode());
//
//        if (body != null) {
//            out.writeBytes(body);
//        }
    }
}
