package com.mumu.game.core.db.util;

import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.redis.constants.SerializerType;
import com.mumu.game.utils.JsonUtil;

/**
 * EntitySerializer
 * 实体序列化工具
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public final class EntitySerializer {

    private EntitySerializer() {
    }

    public static String serialize(ModelMeta meta, Object entity) {
        SerializerType type = resolveSerializer(meta);
        return switch (type) {
            case STRING -> String.valueOf(entity);
            case JSON -> JsonUtil.toJson(entity);
            default -> JsonUtil.toJson(entity);
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(ModelMeta meta, String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        SerializerType type = resolveSerializer(meta);
        try {
            return switch (type) {
                case STRING -> (T) json;
                case JSON -> JsonUtil.fromJson(json, clazz);
                default -> JsonUtil.fromJson(json, clazz);
            };
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "deserialize", "table", meta.getTableName(), "clazz", clazz.getSimpleName());
            return null;
        }
    }

    private static SerializerType resolveSerializer(ModelMeta meta) {
        SerializerType type = meta.getSerializerType();
        if (type == SerializerType.PROTOBUF) {
            LogTopic.MODEL.error("serializerNotSupport", "table", meta.getTableName(), "type", type.name());
            return SerializerType.JSON;
        }
        return type;
    }
}
