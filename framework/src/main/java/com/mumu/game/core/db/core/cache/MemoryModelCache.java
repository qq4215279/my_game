package com.mumu.game.core.db.core.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.mumu.game.collection.LRULinkedHashMap;
import com.mumu.game.core.db.core.inf.ModelCacheReader;
import com.mumu.game.core.db.core.inf.ModelCacheWriter;
import com.mumu.game.core.db.consts.ModelConstants;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.expcetion.ModelArgException;

import lombok.Setter;

/**
 * MemoryModelCache
 * 内存一级缓存（
 * 每个 primaryRouteId 分桶：主存储 HashMap + 非主索引 SecondaryIndex
 * 外层使用 Guava Cache 管理 route 桶：maximumSize + expireAfterAccess，防止业务漏清导致泄漏；
 * 内层 {@code @ModelTable.capacity} 仍约束单桶实体数量。
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public class MemoryModelCache<Entity extends BaseEntity> implements ModelCacheReader, ModelCacheWriter {

    /** primaryRouteId → 路由桶 */
    private final Cache<Long, RouteBucket<Entity>> routeBuckets;
    /** 表元数据 */
    private final ModelMeta meta;

    /**
     * 桶内实体 LRU 淘汰监听（capacity 溢出，语义为丢弃该条并 DELETE 落库）
     */
    @Setter
    private Consumer<Entity> evictionListener;
    /**
     * 外层 route 桶 SIZE/EXPIRED 淘汰监听（primaryRouteId, 桶内实体快照）
     * <p>用于在卸内存前 flush dirty；主动 clearRoute 不会触发</p>
     */
    @Setter
    private BiConsumer<Long, Collection<Entity>> routeEvictionListener;


    public MemoryModelCache(ModelMeta meta) {
        this.meta = meta;
        this.routeBuckets = CacheBuilder.newBuilder()
            .maximumSize(ModelConstants.CACHE_SIZE)
            .expireAfterAccess(ModelConstants.CACHE_DAY, TimeUnit.DAYS)
            .removalListener(this::onRouteBucketRemoved)
            .build();
    }

    /**
     * 外层 route 桶被淘汰时的回调
     */
    private void onRouteBucketRemoved(RemovalNotification<Long, RouteBucket<Entity>> notification) {
        RouteBucket<Entity> bucket = notification.getValue();
        if (bucket == null || bucket.primary.isEmpty()) {
            return;
        }
        RemovalCause cause = notification.getCause();
        // EXPLICIT：clearRoute / 空桶 invalidate，业务已 flush，不再处理
        if (cause == RemovalCause.EXPLICIT || cause == RemovalCause.REPLACED) {
            return;
        }
        Long primaryRouteId = notification.getKey();
        LogTopic.MODEL.info("routeBucketEvicted", "table", meta.getTableName(), "primaryRouteId", primaryRouteId,
            "cause", cause, "size", bucket.primary.size());
        if (routeEvictionListener == null) {
            return;
        }
        try {
            routeEvictionListener.accept(primaryRouteId, List.copyOf(bucket.primary.values()));
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "routeBucketEvictListener", "table", meta.getTableName(), "primaryRouteId", primaryRouteId);
        }
    }

    /** 获取路由桶 */
    private RouteBucket<Entity> getBucket(long primaryRouteId) {
        return routeBuckets.getIfPresent(primaryRouteId);
    }

    /** 获取or创建路由桶 */
    private RouteBucket<Entity> getOrCreateBucket(long primaryRouteId) {
        RouteBucket<Entity> bucket = routeBuckets.getIfPresent(primaryRouteId);
        if (bucket != null) {
            return bucket;
        }
        bucket = createRouteBucket();
        routeBuckets.put(primaryRouteId, bucket);
        return bucket;
    }

    /** 创建路由桶 */
    private RouteBucket<Entity> createRouteBucket() {
        @SuppressWarnings("unchecked")
        RouteBucket<Entity>[] holder = new RouteBucket[1];
        Map<String, Entity> primary = createPrimaryMap(holder);
        RouteBucket<Entity> bucket = new RouteBucket<>(primary, createSecondaryIndexes());
        holder[0] = bucket;
        return bucket;
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
                try {
                    evictionListener.accept(entity);
                } catch (Exception e) {
                    LogTopic.MODEL.error(e, "entityEvictListener", "table", meta.getTableName(),
                            "primaryRouteId", entity.getPrimaryRouteId());
                }
            }
        });
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


    /**
     * 清理指定 primaryRouteId 分桶（EXPLICIT，不触发淘汰 flush）
     */
    public void clearRoute(long primaryRouteId) {
        routeBuckets.invalidate(primaryRouteId);
    }

    public void clearAll() {
        routeBuckets.invalidateAll();
    }

    public boolean hasRoute(long primaryRouteId) {
        RouteBucket<Entity> bucket = getBucket(primaryRouteId);
        return bucket != null && !bucket.primary.isEmpty();
    }




    /**
     * 按完整索引键查询
     */
    public Entity get(long primaryRouteId, IndexMeta index, Object... keys) {
        if (!index.isFullKey(keys)) {
            return null;
        }
        RouteBucket<Entity> bucket = getBucket(primaryRouteId);
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
    public List<Entity> getList(long primaryRouteId , IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0 || keys.length > index.getFields().length) {
            return Collections.emptyList();
        }
        RouteBucket<Entity> bucket = getBucket(primaryRouteId);
        if (bucket == null || bucket.primary.isEmpty()) {
            return Collections.emptyList();
        }

        // 主索引：完整键走 HashMap；左前缀在 route 桶内扫描（桶已按 primaryRouteId 分片）
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
    public List<Entity> getListReverse(long primaryRouteId, IndexMeta index, Object... secondaryKeys) {
        List<Entity> list = new ArrayList<>(getList(primaryRouteId, index, secondaryKeys));
        list.sort(Comparator.comparing((Entity e) -> meta.buildCacheKey(e)).reversed());
        return list;
    }

    /**
     * 写入 JVM 缓存（同 hashField 仅保留一份引用，并同步维护副索引）
     */
    public void put(Entity entity) {
        entity.marshal();
        long primaryRouteId = entity.getPrimaryRouteId();
        RouteBucket<Entity> bucket = getOrCreateBucket(primaryRouteId);
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

    /**
     * 批量写入某个 primaryRouteId 分桶
     */
    @SuppressWarnings("unchecked")
    public void putAll(long primaryRouteId, Map<String, ? extends BaseEntity> fieldEntityMap) {
        for (Map.Entry<String, ? extends BaseEntity> entry : fieldEntityMap.entrySet()) {
            put((Entity) entry.getValue());
        }
    }

    /**
     * 按完整索引键删除（副索引查询后按主存储删除）
     */
    public Entity remove(long primaryRouteId, IndexMeta index, Object... keys) {
        if (!index.isFullKey(keys)) {
            return null;
        }
        RouteBucket<Entity> bucket = getBucket(primaryRouteId);
        if (bucket == null) {
            return null;
        }

        Entity removed;
        if (index.isPrimary()) {
            removed = bucket.primary.remove(meta.buildHashField(index, keys));
        } else {
            SecondaryIndex<Entity> secondary = bucket.secondaryIndexes.get(index.getName());
            if (secondary == null) {
                return null;
            }
            removed = secondary.getOne(index.buildIndexKey(keys));
            if (removed != null) {
                String primaryField = meta.buildHashField(removed, meta.getPrimaryIndex());
                bucket.primary.remove(primaryField);
            }
        }
        if (removed != null) {
            removeFromSecondary(bucket, removed);
            if (bucket.primary.isEmpty()) {
                routeBuckets.invalidate(primaryRouteId);
            }
        }
        return removed;
    }

    /**
     * 按左前缀删除
     */
    public List<Entity> removeAll(IndexMeta primaryIndex, Object... primaryKeys) {
        List<Entity> matched = getList(meta.getRouteId(primaryKeys), primaryIndex, primaryKeys);
        for (Entity entity : matched) {
            removeEntity(entity);
        }
        return matched;
    }

    /**
     * 按实体从主存储 + 副索引移除
     */
    private void removeEntity(Entity entity) {
        long primaryRouteId = entity.getPrimaryRouteId();
        RouteBucket<Entity> bucket = getBucket(primaryRouteId);
        if (bucket == null) {
            return;
        }
        String hashField = meta.buildHashField(entity, meta.getPrimaryIndex());
        Entity removed = bucket.primary.remove(hashField);
        if (removed != null) {
            removeFromSecondary(bucket, removed);
        }
        if (bucket.primary.isEmpty()) {
            routeBuckets.invalidate(primaryRouteId);
        }
    }



    @Override
    public BaseEntity getOne(long primaryRouteId, ModelMeta tableMeta, IndexMeta index, Object... secondaryKeys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        return get(primaryRouteId, index, secondaryKeys);
    }

    @Override
    public List<BaseEntity> getList(long primaryRouteId, ModelMeta tableMeta, IndexMeta index, Object... secondaryKeys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        return new ArrayList<>(getList(primaryRouteId, index, secondaryKeys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(ModelMeta tableMeta, BaseEntity entity) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        put((Entity) entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveBatch(ModelMeta tableMeta, List<? extends BaseEntity> entities) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        for (BaseEntity entity : entities) {
            put((Entity) entity);
        }
    }

    @Override
    public void delete(long primaryRouteId, ModelMeta tableMeta, IndexMeta index, Object... secondaryKeys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        remove(primaryRouteId, index, secondaryKeys);
    }

    @Override
    public void deleteByPrefix(ModelMeta tableMeta, IndexMeta index, Object... keys) {
        if (!meta.getTableName().equals(tableMeta.getTableName())) {
            throw new ModelArgException("JvmModelCache 与 ModelMeta 表名不一致");
        }
        removeAll(index, keys);
    }




    /**
     * 路由分桶：主存储 + 副索引
     * @param primary          主存储：primaryHashField → entity
     * @param secondaryIndexes 副索引：indexName → SecondaryIndex
     */
    private record RouteBucket<Entity extends BaseEntity>(Map<String, Entity> primary,
        Map<String, SecondaryIndex<Entity>> secondaryIndexes) {
    }

}
