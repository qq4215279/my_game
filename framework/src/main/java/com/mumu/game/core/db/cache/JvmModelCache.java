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
 * JVM 一级缓存（每个 routeId 分桶：主存储 HashMap + 非主索引 SecondaryIndex）
 * <p>
 * 主键完整键：O(1)；非主索引完整键：O(1)；左前缀：扫副索引 key（不再全桶 matchPrefix）
 * </p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public class JvmModelCache<Entity extends BaseEntity> implements ModelCacheReader, ModelCacheWriter {
    /**  */
    /** routeId → 路由桶 */
    private final Map<Long, RouteBucket<Entity>> routeBuckets = new HashMap<>();
    /** 表元数据 */
    private final ModelMeta meta;

    /** LRU 淘汰监听器 */
    @Setter
    private Consumer<Entity> evictionListener;

    public JvmModelCache(ModelMeta meta) {
        this.meta = meta;
    }

    /**
     * 按完整索引键查询
     */
    public Entity get(IndexMeta index, Object... keys) {
        if (!index.isFullKey(keys)) {
            return null;
        }
        long routeId = meta.getRouteId(keys);
        RouteBucket<Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null) {
            return null;
        }
        if (index.isPrimary()) {
            return bucket.primary.get(meta.buildHashField(index, keys));
        }
        SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(index.getName());
        return secondary == null ? null : secondary.getOne(index.buildIndexKey(keys));
    }

    /**
     * 按索引键查询列表（完整键或左前缀）
     */
    public List<Entity> list(IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0 || keys.length > index.getFields().length) {
            return Collections.emptyList();
        }
        long routeId = meta.getRouteId(keys);
        RouteBucket<Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null || bucket.primary.isEmpty()) {
            return Collections.emptyList();
        }

        // 主索引：完整键走 HashMap；左前缀在 route 桶内扫描（桶已按 routeId 分片）
        if (index.isPrimary()) {
            if (index.isFullKey(keys)) {
                Entity one = bucket.primary.get(meta.buildHashField(index, keys));
                return one == null ? Collections.emptyList() : List.of(one);
            }
            List<Entity> result = new ArrayList<>();
            for (Entity entity : bucket.primary.values()) {
                if (index.matchPrefix(entity, keys)) {
                    result.add(entity);
                }
            }
            return result;
        }

        // 非主索引：走副索引
        SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(index.getName());
        if (secondary == null) {
            return Collections.emptyList();
        }
        String indexKey = index.buildIndexKey(keys);
        if (index.isFullKey(keys)) {
            return secondary.getAll(indexKey);
        }
        return secondary.leftFind(indexKey);
    }

    /**
     * 倒序列表（按 cacheKey 倒序）
     */
    public List<Entity> listReverse(IndexMeta index, Object... keys) {
        List<Entity> list = new ArrayList<>(list(index, keys));
        list.sort(Comparator.comparing((Entity e) -> meta.buildCacheKey(e)).reversed());
        return list;
    }

    /**
     * 写入 JVM 缓存（同 hashField 仅保留一份引用，并同步维护副索引）
     */
    public void put(Entity entity) {
        entity.marshal();
        long routeId = entity.getPrimaryRouteId();
        RouteBucket<Entity> bucket = routeBuckets.computeIfAbsent(routeId, k -> createRouteBucket());
        String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
        Entity old = bucket.primary.put(hashField, entity);
        // 替换了不同对象：卸旧索引
        if (old != null && old != entity) {
            removeFromSecondary(bucket, old);
        }
        // 新对象或替换：挂新索引（同引用 update 约定索引字段不变，跳过重建）
        if (old != entity) {
            addToSecondary(bucket, entity);
        }
    }

    /**
     * 批量写入某个 routeId 分桶
     */
    public void putAll(long routeId, Map<String, Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (Entity entity : entities.values()) {
            put(entity);
        }
    }

    /**
     * 按完整索引键删除（副索引查询后按主存储删除）
     */
    public Entity remove(IndexMeta primaryIndex, Object... primaryIndexKeys) {
        if (!primaryIndex.isFullKey(primaryIndexKeys)) {
            return null;
        }
        long routeId = meta.getRouteId(primaryIndexKeys);
        RouteBucket<Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null) {
            return null;
        }

        Entity removed;
        if (primaryIndex.isPrimary()) {
            removed = bucket.primary.remove(meta.buildHashField(primaryIndex, primaryIndexKeys));
        } else {
            SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(primaryIndex.getName());
            if (secondary == null) {
                return null;
            }
            removed = secondary.getOne(primaryIndex.buildIndexKey(primaryIndexKeys));
            if (removed != null) {
                String primaryField = meta.buildHashField(removed, meta.getPrimaryIndex());
                bucket.primary.remove(primaryField);
            }
        }
        if (removed != null) {
            removeFromSecondary(bucket, removed);
            if (bucket.primary.isEmpty()) {
                routeBuckets.remove(routeId);
            }
        }
        return removed;
    }

    /**
     * 按左前缀删除
     */
    public List<Entity> removeAll(IndexMeta primaryIndex, Object... primaryKeys) {
        List<Entity> matched = list(primaryIndex, primaryKeys);
        for (Entity entity : matched) {
            removeEntity(entity);
        }
        return matched;
    }

    /**
     * 按实体从主存储 + 副索引移除
     */
    private void removeEntity(Entity entity) {
        long routeId = entity.getPrimaryRouteId();
        RouteBucket<Entity> bucket = routeBuckets.get(routeId);
        if (bucket == null) {
            return;
        }
        String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
        Entity removed = bucket.primary.remove(hashField);
        if (removed != null) {
            removeFromSecondary(bucket, removed);
        }
        if (bucket.primary.isEmpty()) {
            routeBuckets.remove(routeId);
        }
    }

    /**
     * 清理指定 routeId 分桶（副索引随桶一起丢弃）
     */
    public void clearRoute(long routeId) {
        routeBuckets.remove(routeId);
    }

    public void clear() {
        routeBuckets.clear();
    }

    public boolean hasRoute(long routeId) {
        RouteBucket<Entity> bucket = routeBuckets.get(routeId);
        return bucket != null && !bucket.primary.isEmpty();
    }

    private RouteBucket<Entity> createRouteBucket() {
        @SuppressWarnings("unchecked")
        RouteBucket<Entity>[] holder = new RouteBucket[1];
        Map<String, Entity> primary = createPrimaryMap(holder);
        RouteBucket<Entity> bucket = new RouteBucket<>(primary, createSecondaryIndexes());
        holder[0] = bucket;
        return bucket;
    }

    private Map<String, Entity> createPrimaryMap(RouteBucket<Entity>[] holder) {
        int capacity = meta.getCapacity();
        if (capacity >= Integer.MAX_VALUE) {
            return new HashMap<>();
        }
        return LRULinkedHashMap.of(capacity, (field, entity) -> {
            RouteBucket<Entity> bucket = holder[0];
            if (bucket != null) {
                // LRU 淘汰主存储时同步卸副索引，避免脏引用
                removeFromSecondary(bucket, entity);
            }
            if (evictionListener != null) {
                evictionListener.accept(entity);
            }
        });
    }

    private Map<String, SecondaryIndex<Entity>> createSecondaryIndexes() {
        if (!meta.hasSecondaryIndex()) {
            return Collections.emptyMap();
        }
        Map<String, SecondaryIndex<Entity>> map = new HashMap<>();
        for (IndexMeta index : meta.getSecondaryIndexes()) {
            map.put(index.getName(), new SecondaryIndex<>(index.isUnique()));
        }
        return map;
    }

    private void addToSecondary(RouteBucket<Entity> bucket, Entity entity) {
        if (!meta.hasSecondaryIndex()) {
            return;
        }
        for (IndexMeta index : meta.getSecondaryIndexes()) {
            SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(index.getName());
            if (secondary != null) {
                secondary.put(index.buildIndexKey(entity), entity);
            }
        }
    }

    private void removeFromSecondary(RouteBucket<Entity> bucket, Entity entity) {
        if (!meta.hasSecondaryIndex()) {
            return;
        }
        for (IndexMeta index : meta.getSecondaryIndexes()) {
            SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(index.getName());
            if (secondary != null) {
                secondary.remove(index.buildIndexKey(entity), entity);
            }
        }
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
    public void save(ModelMeta meta, BaseEntity entity) {
        if (!this.meta.getTableName().equals(meta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        put((Entity) entity);
    }

    @Override
    public void saveBatch(ModelMeta meta, List<? extends BaseEntity> entities) {
        if (!meta.getTableName().equals(meta.getTableName())) {
            throw new IllegalArgumentException("JvmModelCache 与 ModelMeta 表名不一致");
        }

        for (BaseEntity entity : entities) {
            put((Entity) entity);
        }
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

    /**
     * 路由分桶：主存储 + 副索引
     */
    private static final class RouteBucket<Entity extends BaseEntity> {
        /** 主存储：primaryHashField → entity */
        private final Map<String, Entity> primary;
        /** 副索引：indexName → SecondaryIndex */
        private final Map<String, SecondaryIndex<Entity>> secondaryIndexes;

        private RouteBucket(Map<String, Entity> primary, Map<String, SecondaryIndex<Entity>> secondaryIndexes) {
            this.primary = primary;
            this.secondaryIndexes = secondaryIndexes;
        }
    }
}
