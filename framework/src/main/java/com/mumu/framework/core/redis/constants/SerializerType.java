package com.mumu.framework.core.redis.constants;

import com.mumu.common.util2.JsonUtil;
import java.nio.charset.StandardCharsets;

/**
 * SerializerType Redis序列化方式
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:39
 */
public enum SerializerType {
  /** 字符串 */
  STRING {
    @Override
    public byte[] serialize(Object msg) {
      return msg.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(byte[] msg, Class<?> clazz) {
      return new String(msg, StandardCharsets.UTF_8);
    }
  },
  /** JSON */
  JSON {
    @Override
    public byte[] serialize(Object msg) {
      return JsonUtil.toJson(msg).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(byte[] msg, Class<?> clazz) {
      return JsonUtil.fromJson(new String(msg, StandardCharsets.UTF_8), clazz);
    }
  },
  /** protoBuf */
  PROTOBUF {
    @Override
    public byte[] serialize(Object msg) {
      // return JProtoBufUtil.encode(msg);
      // TODO
      return null;
    }

    @Override
    public Object deserialize(byte[] msg, Class<?> clazz) {
      // return JProtoBufUtil.decode(msg, clazz);
      // TODO
      return null;
    }
  },
  ;

  public abstract byte[] serialize(Object msg);

  public abstract Object deserialize(byte[] msg, Class<?> clazz);
}
