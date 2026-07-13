package com.mumu.game.core.db.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.cache.inf.ModelCacheReader;
import com.mumu.game.core.db.cache.inf.ModelCacheWriter;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.util.EntitySerializer;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.redis.RedisUtil;

/**
 * RedisModelCache
 * Redis 二级缓存
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class RedisModelCache implements ModelCacheReader, ModelCacheWriter {

    /**
     * 批量加载某个 routeId 的 Redis Hash 桶（preload 使用）
     */
    public <Entity extends BaseEntity> Map<String, Entity> loadRouteBucket(ModelMeta meta, Class<Entity> clazz, long routeId) {
        if (!meta.hasRedis()) {
            return Collections.emptyMap();
        }
        try {
            String redisKey = meta.buildRedisKey(routeId);
            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Entity> result = new HashMap<>(all.size());
            for (Map.Entry<String, String> entry : all.entrySet()) {
                Entity entity = EntitySerializer.deserialize(meta, entry.getValue(), clazz);
                if (entity != null) {
                    entity.marshal();
                    result.put(entry.getKey(), entity);
                }
            }
            return result;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisLoadRoute", "table", meta.getTableName(), "routeId", routeId);
            return Collections.emptyMap();
        }
    }

    @Override
    public <T extends BaseEntity> T getOne(ModelMeta meta, IndexMeta index, Class<T> clazz, Object... keys) {
        if (!meta.hasRedis() || !index.isFullKey(keys)) {
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
            T entity = EntitySerializer.deserialize(meta, json, clazz);
            if (entity != null) {
                entity.marshal();
            }
            return entity;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisGet", "table", meta.getTableName(), "index", index.getName());
            return null;
        }
    }

    @Override
    public <T extends BaseEntity> List<T> getList(ModelMeta meta, IndexMeta index, Class<T> clazz, Object... keys) {
        if (!meta.hasRedis()) {
            return Collections.emptyList();
        }
        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, T> matched = new HashMap<>();
            for (Map.Entry<String, String> entry : all.entrySet()) {
                T entity = EntitySerializer.deserialize(meta, entry.getValue(), clazz);
                if (entity == null) {
                    continue;
                }
                entity.marshal();
                if (index.matchPrefix(entity, keys)) {
                    matched.put(entry.getKey(), entity);
                }
            }
            return List.copyOf(matched.values());
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisList", "table", meta.getTableName(), "index", index.getName());
            return Collections.emptyList();
        }
    }

    @Override
    public void save(ModelMeta meta, BaseEntity entity) {
        if (!meta.hasRedis()) {
            return;
        }
        try {
            entity.unmarshal();
            long routeId = entity.getPrimaryRouteId();
            String redisKey = meta.buildRedisKey(routeId);
            String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
            String json = EntitySerializer.serialize(meta, entity);
            boolean success = RedisUtil.hset(redisKey, hashField, json, meta.getExpire());
            if (!success) {
                LogTopic.MODEL.error("redisSaveFail", "table", meta.getTableName(), "key", redisKey, "field", hashField);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisSave", "table", meta.getTableName());
            throw new IllegalStateException("Redis 写入失败: " + meta.getTableName(), e);
        }
    }

    /**
     * 批量保存到 Redis（同一 routeId 桶一次 hmset）
     */
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
            throw new IllegalStateException("Redis 批量写入失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void delete(ModelMeta meta, IndexMeta index, Object... keys) {
        if (!meta.hasRedis() || !index.isFullKey(keys)) {
            return;
        }
        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            String hashField = meta.buildHashField(index, keys);
            RedisUtil.hdel(redisKey, hashField);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisDelete", "table", meta.getTableName(), "index", index.getName());
            throw new IllegalStateException("Redis 删除失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void deleteByPrefix(ModelMeta meta, IndexMeta index, Object... keys) {
        if (!meta.hasRedis()) {
            return;
        }
        try {
            long routeId = meta.getRouteId(keys);
            String redisKey = meta.buildRedisKey(routeId);
            Map<String, String> all = RedisUtil.hmget(redisKey);
            if (all.isEmpty()) {
                return;
            }
            for (String field : all.keySet()) {
                if (matchFieldByPrefix(meta, index, field, keys)) {
                    RedisUtil.hdel(redisKey, field);
                }
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "redisDeleteAll", "table", meta.getTableName(), "index", index.getName());
            throw new IllegalStateException("Redis 批量删除失败: " + meta.getTableName(), e);
        }
    }

    private boolean matchFieldByPrefix(ModelMeta meta, IndexMeta index, String hashField, Object... keys) {
        String[] parts = hashField.split(":");
        if (keys.length > parts.length) {
            return false;
        }
        if (!meta.isSingleFieldPrimary() && index == meta.getPrimaryIndex()) {
            for (int i = 0; i < keys.length; i++) {
                int partIndex = i + 1;
                if (partIndex >= parts.length) {
                    return false;
                }
                if (!String.valueOf(keys[i]).equals(parts[partIndex])) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < keys.length; i++) {
            if (!String.valueOf(keys[i]).equals(parts[i])) {
                return false;
            }
        }
        return true;
    }
}
