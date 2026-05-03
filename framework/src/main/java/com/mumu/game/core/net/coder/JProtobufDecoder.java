package com.mumu.game.core.net.coder;

import java.util.List;

import com.mumu.game.core.utils.JProtoBufUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * JProtobufDecoder
 * JProtobuf解码器，用于将Netty接收到的ByteBuf二进制数据解码为GameMessagePackage对象
 * <p>该解码器使用百度JProtobuf库进行Protocol Buffer反序列化</p>
 * @author liuzhen
 * @version 1.0.0 2026/5/2 23:24
 */
@ChannelHandler.Sharable
public class JProtobufDecoder extends MessageToMessageDecoder<ByteBuf> {

    /**
     * 解码方法：将ByteBuf转换为GameMessagePackage对象
     *
     * 处理流程：
     *   获取ByteBuf中可读数据的长度
     *   根据ByteBuf的实现类型，选择最优方式获取字节数组（避免不必要的内存拷贝）
     *   调用JProtobuf工具类将字节数组解码为GameMessagePackage对象
     *   将解码后的对象添加到输出列表，传递给下一个ChannelHandler
     *
     * @param channelHandlerContext 通道上下文，包含通道状态和配置信息
     * @param msg 输入的ByteBuf，包含从网络接收到的原始protobuf二进制数据
     * @param out 输出列表，解码后的GameMessagePackage对象会被添加到此列表
     * @throws Exception 解码过程中可能抛出的异常
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg, List<Object> out)
            throws Exception {
        // 获取ByteBuf中可读的字节数
        int length = msg.readableBytes();

        // 存储数据的字节数组
        byte[] array;
        // 数据在数组中的起始偏移量
        int offset;

        // 判断ByteBuf是否有可访问的底层数组（如UnpooledHeapByteBuf）
        if (msg.hasArray()) {
            // 有底层数组：直接引用，避免内存拷贝，提升性能
            array = msg.array();
            // 计算实际数据的起始位置：数组基础偏移量 + 当前读索引
            offset = msg.arrayOffset() + msg.readerIndex();
        } else {
            // 无底层数组（如DirectByteBuf、CompositeByteBuf等）：必须拷贝数据
            // getBytes参数：源ByteBuf、起始位置、长度、是否释放源缓冲区
            array = ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false);
            // 新数组从索引0开始
            offset = 0;
        }

        // 使用JProtobuf将字节数组解码为GameMessagePackage对象
        // decodeMessageProxy会提取array[offset, offset+length]范围的数据进行解码
        // GameMessagePackage包含消息头(header)和消息体(body)
        out.add(JProtoBufUtil.decodeMessageProxy(array, offset, length));
    }


    public static JProtobufDecoder getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final JProtobufDecoder INSTANCE = new JProtobufDecoder();
    }
}