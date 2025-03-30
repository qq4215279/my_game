/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufIDLProxy;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.log.LogTopic;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;

/**
 * JProtoBufUtil
 * JProtobuf解码器
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:16
 */
@SuppressWarnings({"all"})
public class JProtoBufUtil {
    public static byte[] NONE = new byte[0];

    /** 获取协议的codec */
    public static Codec createCodec(Class protoClass) {
        try {
            return ProtobufProxy.create(protoClass);
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "JProtobuf CreateCodec", "protoClass", protoClass);
        }
        return null;
    }

    /** 将 JProtoBuf 对象编码为 byte[] */
    public static byte[] encode(Object data) {
        if (data == null) return NONE;
        if (data instanceof byte[] bytes) return bytes;
        try {
            return createCodec((Class) data.getClass()).encode(data);
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "JProtobuf encode", "data", data);
            return NONE;
        }
    }

    /** 将 byte[] 解码为 JProtoBuf对象 */
    @SuppressWarnings({"all"})
    public static <T> T decode(byte[] data, Class<T> clazz) {
        if (data == null) data = NONE;
        try {
            return (T) createCodec(clazz).decode(data);
        } catch (Exception e) {
            LogTopic.ACTION.error(e, "JProtobuf decode", "clazz", clazz, "data", Arrays.toString(data));
            return null;
        }
    }

    /** 解码 GameMessagePackage */
    public static GameMessagePackage decodeMessageProxy(byte[] data, int off, int len) {
        return decode(ArrayUtil.sub(data, off, len), GameMessagePackage.class);
    }

    // ----------------------------- 测试 ----------------------------------

    private static void testDecode() {
        // OnChangePlayerPosition bean = new OnChangePlayerPosition();
        // bean.setOptType(OptType.ADD);
        // bean.setServerGroup("World");
        //
        // byte[] encode = encode(bean);
        // System.out.println(encode.length);
        // MessageProxy proxy = MessageSender.proxy(Cmd.ECPong, bean);
        // byte[] proxyEncode = encode(proxy);
        //
        // MessageProxy proxyNew = decodeMessageProxy(proxyEncode, 0, proxyEncode.length);
        // System.out.println(proxyNew);
        // OnChangePlayerPosition bean2 = decode(proxyNew.getData(), OnChangePlayerPosition.class);
        // System.out.println(bean2);
    }

    /** proto 文件生成 java 文件 */
    private static void protoToJava(String protoFile) throws IOException {
        URL resource = ResourceUtil.getResource("proto/" + protoFile);
        System.out.println(resource.getPath());
        ProtobufIDLProxy.generateSource(resource.openStream(), new File("D:\\TestFiles\\aa"));
    }

    public static void main(String[] args) {
        testDecode();
    }
}
