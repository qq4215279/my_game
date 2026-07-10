package com.mumu.game.core.db.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mumu.game.collection.LRULinkedHashMap;
import com.mumu.game.core.db.cache.inf.ModelCacheReader;
import com.mumu.game.core.db.cache.inf.ModelCacheWriter;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;

import lombok.Setter;

/**
 * JvmModelCache
 * JVM 一级缓存（每个 routeId 分桶，桶内每条记录仅保留一份对象）
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public class JvmModelCache<Entity extends BaseEntity> implements ModelCacheReader, ModelCacheWriter {

    /** routeId -> hashField -> entity */
    private final Map<Long, Map<String, Entity>> routeBuckets = new HashMap<>();
    /** 表元数据 */
    private final ModelMeta meta;

    /** LRU 淘汰监听器 */
    @Setter
    private Consumer<Entity> evictionListener;

    public JvmModelCache(ModelMeta meta) {
        this.meta = meta;
    }

    /**
     * 按完整索引键查询（内部方法，供 BaseModel 高频调用）
     */
    public Entity get(IndexMeta index, Object... keys) {
        if (!index.isFullKey(keys)) {
            return null;
        }
        long routeId = meta.getRouteId(keys);
        String hashField = meta.buildHashField(index, keys);
        Map<String, Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null) {
            return null;
        }
        return bucket.get(hashField);
    }

    /**
     * 按左前缀查询列表（内部方法）
     */
    public List<Entity> list(IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0 || keys.length > index.getFields().length) {
            return Collections.emptyList();
        }
        long routeId = meta.getRouteId(keys);
        Map<String, Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null || bucket.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entity> result = new ArrayList<>();
        for (Entity entity : bucket.values()) {
            if (index.matchPrefix(entity, keys)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 倒序列表（按 cacheKey 倒序）
     */
    public List<Entity> listReverse(IndexMeta index, Object... keys) {
        List<Entity> list = new ArrayList<>(list(index, keys));
        list.sort(Comparator.comparing(meta::buildCacheKey).reversed());
        return list;
    }

    /**
     * 写入 JVM 缓存（同 hashField 仅保留一份引用）
     */
    public void put(Entity entity) {
        long routeId = meta.getRouteId(entity);
        String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
        Map<String, Entity> bucket = routeBuckets.computeIfAbsent(routeId, k -> createBucket());
        bucket.put(hashField, entity);
    }

    /**
     * 批量写入某个 routeId 分桶
     */
    public void putAll(long routeId, Map<String, Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, Entity> bucket = routeBuckets.computeIfAbsent(routeId, k -> createBucket());
        bucket.putAll(entities);
    }

    /**
     * 创建 routeId 分桶。
     * 有容量限制时使用 LRULinkedHashMap（插入序淘汰最久元素，与 @ModelTable.capacity 语义一致）
     */
    private Map<String, Entity> createBucket() {
        int capacity = meta.getCapacity();
        if (capacity >= Integer.MAX_VALUE) {
            return new HashMap<>();
        }
        return LRULinkedHashMap.of(capacity, (field, entity) -> {
            if (evictionListener != null) {
                evictionListener.accept(entity);
            }
        });
    }

    /**
     * 按完整索引键删除
     */
    public Entity remove(IndexMeta index, Object... keys) {
        if (!index.isFullKey(keys)) {
            return null;
        }
        long routeId = meta.getRouteId(keys);
        String hashField = meta.buildHashField(index, keys);
        Map<String, Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null) {
            return null;
        }
        Entity removed = bucket.remove(hashField);
        if (bucket.isEmpty()) {
            routeBuckets.remove(routeId);
        }
        return removed;
    }

    /**
     * 按左前缀删除
     */
    public List<Entity> removeAll(IndexMeta index, Object... keys) {
        List<Entity> matched = list(index, keys);
        for (Entity entity : matched) {
            remove(index, index.readKeyValues(entity));
        }
        return matched;
    }

    /**
     * 清理指定 routeId 分桶
     */
    public void clearRoute(long routeId) {
        routeBuckets.remove(routeId);
    }

    public void clear() {
        routeBuckets.clear();
    }

    public boolean hasRoute(long routeId) {
        Map<String, Entity> bucket = routeBuckets.get(routeId);
        return bucket != null && !bucket.isEmpty();
    }

    @Override
    public <E extends BaseEntity> E getOne(ModelMeta tableMeta, IndexMeta index, Class<E> clazz, Object... keys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        return clazz.cast(get(index, keys));
    }

    @Override
    public <E extends BaseEntity> List<E> getList(ModelMeta tableMeta, IndexMeta index, Class<E> clazz,
                                                  Object... keys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        List<Entity> list = list(index, keys);
        List<E> result = new ArrayList<>(list.size());
        for (Entity entity : list) {
            result.add(clazz.cast(entity));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(ModelMeta tableMeta, BaseEntity entity) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        put((Entity) entity);
    }

    @Override
    public void delete(ModelMeta tableMeta, IndexMeta index, Object... keys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        remove(index, keys);
    }

    @Override
    public void deleteByPrefix(ModelMeta tableMeta, IndexMeta index, Object... keys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        removeAll(index, keys);
    }
}
