package com.mumu.game.core.db.core.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.bootstrap.ModelRegistry;
import com.mumu.game.core.db.core.inf.ModelCacheReader;
import com.mumu.game.core.db.core.inf.ModelCacheWriter;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.util.EntitySerializer;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.redis.RedisUtil;
import com.mumu.game.expcetion.ModelPersistException;

/**
 * RedisModelCache
 * Redis 二级缓存（共享层，统一使用 {@link BaseEntity}，不做业务泛型）
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class RedisModelCache implements ModelCacheReader, ModelCacheWriter {

    /**
     * 批量加载某个 primaryRouteId 的 Redis 数据（preload / ensureRouteLoaded 使用）
     *   <li>单字段主键（ONE）：整表共用一个 Hash，只取 field={primaryRouteId}</li>
     *   <li>复合主键（MANY）：Hash 已按 route 分桶，hmget 整桶</li>
     */
    public Map<String, BaseEntity> loadRouteBucket(ModelMeta meta, Class<? extends BaseEntity> clazz, long primaryRouteId) {
        if (!meta.hasRedis()) {
            return Collections.emptyMap();
        }
        try {
            String redisKey = meta.buildRedisKey(primaryRouteId);
            // 1. 单字段主键（ONE）：避免把全表 Hash 灌进某一个 route 桶
            if (meta.isSingleFieldPrimary()) {
                String hashField = String.valueOf(primaryRouteId);
                String json = RedisUtil.hGet(redisKey, hashField, String::valueOf);
                if (json == null) {
                    return Collections.emptyMap();
                }
                BaseEntity entity = EntitySerializer.deserialize(meta, json, clazz);
                if (entity == null) {
                    return Collections.emptyMap();
                }
                entity.marshal();
                return Map.of(hashField, entity);
            }

            // 2. 复合主键（MANY）
            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, BaseEntity> result = new HashMap<>(all.size());
            for (Map.Entry<String, String> entry : all.entrySet()) {
                BaseEntity entity = EntitySerializer.deserialize(meta, entry.getValue(), clazz);
                if (entity != null) {
                    entity.marshal();
                    result.put(entry.getKey(), entity);
                }
            }
            return result;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisLoadRouteBucket", "table", meta.getTableName(), "routeId", primaryRouteId);
            return Collections.emptyMap();
        }
    }

    @Override
    public BaseEntity getOne(long primaryRouteId, IndexMeta index, Object... keys) {
        ModelMeta meta = ModelRegistry.getMeta(index.getEntityClass());
        if (!meta.hasRedis() || !index.isFullKey(keys)) {
            return null;
        }
        // 非主索引
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("redisGetOne", "非主索引无法从redis中查询", "table", meta.getTableName(), "index",
                    index.getName(), "routeId", primaryRouteId);
            return null;
        }

        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            String hashField = meta.buildHashField(index, keys);
            String json = RedisUtil.hGet(redisKey, hashField, String::valueOf);
            if (json == null) {
                return null;
            }
            BaseEntity entity = EntitySerializer.deserialize(meta, json, entityClass(meta));
            if (entity != null) {
                entity.marshal();
            }
            return entity;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisGet", "table", meta.getTableName(), "index", index.getName(), "routeId", primaryRouteId);
            return null;
        }
    }

    @Override
    public List<BaseEntity> getList(long primaryRouteId, IndexMeta index, Object... keys) {
        ModelMeta meta = ModelRegistry.getMeta(index.getEntityClass());
        if (!meta.hasRedis()) {
            return Collections.emptyList();
        }
        // 非主索引
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("redisGetList", "非主索引无法从redis中查询", "table", meta.getTableName(), "index",
                    index.getName(), "routeId", primaryRouteId);
            return Collections.emptyList();
        }

        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return Collections.emptyList();
            }

            Class<? extends BaseEntity> clazz = entityClass(meta);
            List<BaseEntity> matched = new ArrayList<>();
            for (Map.Entry<String, String> entry : all.entrySet()) {
                BaseEntity entity = EntitySerializer.deserialize(meta, entry.getValue(), clazz);
                if (entity == null) {
                    continue;
                }
                entity.marshal();
                if (index.matchPrefix(entity, keys)) {
                    matched.add(entity);
                }
            }
            return matched;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisGetList", "table", meta.getTableName(), "index", index.getName(), "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    @Override
    public void save(ModelMeta meta, BaseEntity entity) {
        if (!meta.hasRedis()) {
            return;
        }
        long routeId = 0L;
        try {
            entity.unmarshal();
            routeId = entity.getPrimaryRouteId();
            String redisKey = meta.buildRedisKey(routeId);
            String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
            String json = EntitySerializer.serialize(meta, entity);
            boolean success = RedisUtil.hset(redisKey, hashField, json, meta.getExpire());
            if (!success) {
                LogTopic.MODEL.error("redisSaveFail", "table", meta.getTableName(), "routeId", routeId, "key", redisKey,
                        "field", hashField);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisSave", "table", meta.getTableName(), "routeId", routeId);
            throw new ModelPersistException("Redis 写入失败: " + meta.getTableName(), e);
        }
    }

    /**
     * 批量保存到 Redis（同一 routeId 桶一次 hmset）
     */
    @Override
    public void saveBatch(ModelMeta meta, List<? extends BaseEntity> entities) {
        if (!meta.hasRedis() || entities == null || entities.isEmpty()) {
            return;
        }
        try {
            Map<Long, Map<String, String>> routeBuckets = new HashMap<>();
            for (BaseEntity entity : entities) {
                entity.unmarshal();
                long routeId = entity.getPrimaryRouteId();
                String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
                String json = EntitySerializer.serialize(meta, entity);
                routeBuckets.computeIfAbsent(routeId, k -> new HashMap<>()).put(hashField, json);
            }
            for (Map.Entry<Long, Map<String, String>> entry : routeBuckets.entrySet()) {
                String redisKey = meta.buildRedisKey(entry.getKey());
                boolean success = RedisUtil.hmset(redisKey, entry.getValue());
                if (meta.getExpire() > 0) {
                    RedisUtil.expire(redisKey, meta.getExpire());
                }
                if (!success) {
                    LogTopic.MODEL.error("redisSaveBatchFail", "table", meta.getTableName(), "key", redisKey,
                        "size", entry.getValue().size());
                }
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisSaveBatch", "table", meta.getTableName());
            throw new ModelPersistException("Redis 批量写入失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void delete(long primaryRouteId, IndexMeta index, Object... keys) {
        ModelMeta meta = ModelRegistry.getMeta(index.getEntityClass());
        if (!meta.hasRedis() || !index.isFullKey(keys)) {
            return;
        }
        // 仅支持主索引完整键删除（Redis Hash Field 按主索引构建）
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("redisDelete", "仅支持主索引删除", "table", meta.getTableName(), "routeId", primaryRouteId, "index", index.getName());
            return;
        }

        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            String hashField = meta.buildHashField(index, keys);
            RedisUtil.hdel(redisKey, hashField);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisDelete", "table", meta.getTableName(), "index", index.getName(), "routeId", primaryRouteId);
            throw new ModelPersistException("Redis 删除失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public List<BaseEntity> deleteByPrefix(IndexMeta index, Object... keys) {
        ModelMeta meta = ModelRegistry.getMeta(index.getEntityClass());
        if (!meta.hasRedis() || keys == null || keys.length == 0) {
            return Collections.emptyList();
        }
        // 仅支持主索引完整键删除（Redis Hash Field 按主索引构建）
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("redisDeleteByPrefix", "仅支持主索引删除", "table", meta.getTableName(), "index", index.getName());
            return Collections.emptyList();
        }

        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);

            // 复合主键 + 仅传 routeId：该 Hash 整桶都属于此分片，直接删 key
            if (!meta.isSingleFieldPrimary() && keys.length == 1) {
                RedisUtil.del(redisKey);
                return Collections.emptyList();
            }

            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return Collections.emptyList();
            }
            for (String field : all.keySet()) {
                if (matchFieldByPrefix(meta, index, field, keys)) {
                    RedisUtil.hdel(redisKey, field);
                }
            }
            // Redis 侧不反序列化实体，dirty 清理依赖 Memory 返回值
            return Collections.emptyList();
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisDeleteByPrefix", "table", meta.getTableName(), "index", index.getName());
            throw new ModelPersistException("Redis 批量删除失败: " + meta.getTableName(), e);
        }
    }

    /**
     * 判断 Redis Hash Field 是否匹配索引左前缀
     * <p>
     * 复合主索引的 field 不含 routeId（route 已在 redisKey 中）；
     * 单字段主索引 / 其他情况按 field 分段与 keys 对齐比较。
     * </p>
     */
    private boolean matchFieldByPrefix(ModelMeta meta, IndexMeta index, String hashField, Object... keys) {
        String[] parts = hashField.isEmpty() ? new String[0] : hashField.split(":");

        if (!meta.isSingleFieldPrimary()) {
            // keys[0]=routeId，field 从 keys[1] 起匹配
            if (keys.length <= 1) {
                return true;
            }
            int fieldKeyCount = keys.length - 1;
            if (fieldKeyCount > parts.length) {
                return false;
            }
            for (int i = 0; i < fieldKeyCount; i++) {
                if (!String.valueOf(keys[i + 1]).equals(parts[i])) {
                    return false;
                }
            }
            return true;
        }

        if (keys.length > parts.length) {
            return false;
        }
        for (int i = 0; i < keys.length; i++) {
            if (!String.valueOf(keys[i]).equals(parts[i])) {
                return false;
            }
        }
        return true;
    }



    @SuppressWarnings("unchecked")
    private Class<? extends BaseEntity> entityClass(ModelMeta meta) {
        return (Class<? extends BaseEntity>) meta.getEntityClass();
    }
}
