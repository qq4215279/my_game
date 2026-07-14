package com.mumu.game.core.db.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.cache.MemoryModelCache;
import com.mumu.game.core.db.cache.RedisModelCache;
import com.mumu.game.core.db.consts.PersistOp;
import com.mumu.game.core.db.dirty.DirtyEntry;
import com.mumu.game.core.db.dirty.DirtyTracker;
import com.mumu.game.core.db.engine.PersistEngineFactory;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.pool.PersistThreadPool;
import com.mumu.game.core.db.util.ModelRouteChecker;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.expcetion.ModelArgException;

import jakarta.annotation.Resource;

/**
 * BaseModel
 * 数据模型通用实现
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public abstract class BaseModel<Entity extends BaseEntity> implements Model<Entity>, ModelHook<Entity> {
    /** 表元数据 */
    private ModelMeta meta;
    /** 内存缓存 */
    private MemoryModelCache<Entity> memoryCache;
    @Resource
    private RedisModelCache redisModelCache;

    @Resource
    private DirtyTracker dirtyTracker;
    @Resource
    private PersistThreadPool persistThreadPool;
    @Resource
    private PersistEngineFactory persistEngineFactory;
    @Resource
    private ModelRouteChecker modelRouteChecker;


    @Override
    public Entity selectOne(Object... primaryKeys) {
        long primaryRouteId = 0L;
        try {
            primaryRouteId = meta.getRouteId(primaryKeys);
            return selectOne(primaryRouteId, meta.getPrimaryIndex().getName(), primaryKeys);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectOne", "table", meta.getTableName(), "routeId", primaryRouteId);
            return null;
        }
    }

    @Override
    public Entity selectOne(long primaryRouteId, String indexName, Object... secondaryKeys) {
        try {
            IndexMeta index = meta.getIndex(indexName);
            if (!index.isFullKey(secondaryKeys)) {
                throw new ModelArgException("selectOne 需要完整索引键");
            }

            // 1. 内存查询
            Entity local = memoryCache.get(primaryRouteId, index, secondaryKeys);
            if (local != null) {
                return local;
            }
            // 2. redis查询
            if (meta.hasRedis()) {
                Entity redis = castEntity(redisModelCache.getOne(primaryRouteId, meta, index, secondaryKeys));
                // 缓存到内存
                if (redis != null) {
                    memoryCache.put(redis);
                    return redis;
                }
            }
            // 3. db查询
            if (meta.hasDb()) {
                return loadAndCacheFromDb(index, primaryRouteId);
            }

            return null;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectOne", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return null;
        }
    }

    /** 从db加载并缓存到内存/redis */
    private Entity loadAndCacheFromDb(IndexMeta index, Object... primaryKeys) {
        if (!meta.hasDb()) {
            return null;
        }
        // 非主索引
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("loadListFromDb", "非主索引无法从db中查询", "table", meta.getTableName(), "index",
                    index.getName());
            return null;
        }
        Entity db = persistEngineFactory.getEngine(meta).findOne(meta, index, entityClass(), primaryKeys);
        if (db != null) {
            cacheEntity(db);
        }
        return db;
    }

    /** DB/Redis 回填：写入 L1，并同步回填 L2 */
    private void cacheEntity(Entity entity) {
        if (meta.hasJVM()) {
            memoryCache.put(entity);
        }
        saveToRedisQuietly(entity);
    }

    /** 回写到redis */
    private void saveToRedisQuietly(Entity entity) {
        if (!meta.hasRedis()) {
            return;
        }
        try {
            redisModelCache.save(meta, entity);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "saveToRedisQuietly", "table", meta.getTableName());
        }
    }

    @Override
    public List<Entity> selectList(Object... primaryKeys) {
        long primaryRouteId = 0L;
        try {
            primaryRouteId = meta.getRouteId(primaryKeys);
            return selectList(primaryRouteId, meta.getPrimaryIndex().getName(), primaryKeys);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectList", "table", meta.getTableName(), "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Entity> selectList(long primaryRouteId, String indexName, Object... secondaryKeys) {
        try {
            IndexMeta index = meta.getIndex(indexName);
            // 1. 内存查询
            if (meta.hasJVM()) {
                List<Entity> local = memoryCache.getList(primaryRouteId, index, secondaryKeys);
                if (!local.isEmpty()) {
                    return local;
                }
            }
            // 2. redis查询
            if (meta.hasRedis()) {
                List<Entity> redisList = castList(redisModelCache.getList(primaryRouteId, meta, index, secondaryKeys));
                // 缓存到内存
                cache2Jvm(redisList);
                return redisList;
            }
            // 3. db查询
            if (meta.hasDb()) {
                return loadAndCacheListFromDb(index, meta.getRouteId(secondaryKeys));
            }

            return Collections.emptyList();
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectList", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    /** 从db加载并缓存到内存/redis */
    private List<Entity> loadAndCacheListFromDb(IndexMeta index, long primaryRouteId) {
        if (!meta.hasDb()) {
            return Collections.emptyList();
        }
        // 非主索引
        if (!index.isPrimary()) {
            LogTopic.MODEL.error("loadListFromDb", "非主索引无法从db中查询", "table", meta.getTableName(), "index",
                    index.getName());
            return Collections.emptyList();
        }

        List<Entity> dbList = persistEngineFactory.getEngine(meta).findList(meta, index, entityClass(), primaryRouteId);
        if (!dbList.isEmpty()) {
            // 缓存到内存
            cache2Jvm(dbList);
            // 缓存到redis
            cache2Redis(dbList);
        }
        return dbList;
    }

    /** 缓存到内存 */
    private void cache2Jvm(List<Entity> entities) {
        // 缓存到内存
        if (meta.hasJVM()) {
            for (Entity entity : entities) {
                memoryCache.put(entity);
            }
        }
    }

    /** 缓存到redis */
    private void cache2Redis(List<Entity> entities) {
        if (!meta.hasRedis()) {
            return;
        }
        try {
            redisModelCache.saveBatch(meta, entities);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "cache2Redis saveBatch error", "table", meta.getTableName());
        }
    }

    @Override
    public List<Entity> selectListReverse(Object... primaryKeys) {
        long primaryRouteId = 0L;
        try {
            primaryRouteId = meta.getRouteId(primaryKeys);
            return selectListReverse(primaryRouteId, meta.getPrimaryIndex().getName(), primaryKeys);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectListReverse", "table", meta.getTableName(), "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Entity> selectListReverse(long primaryRouteId, String indexName, Object... secondaryKeys) {
        try {
            IndexMeta index = meta.getIndex(indexName);
            if (index.isFullKey(secondaryKeys)) {
                Entity one = selectOne(primaryRouteId, indexName, secondaryKeys);
                return one == null ? Collections.emptyList() : List.of(one);
            }
            List<Entity> local = memoryCache.getListReverse(primaryRouteId, index, secondaryKeys);
            if (!local.isEmpty()) {
                return local;
            }
            List<Entity> list = selectList(primaryRouteId, indexName, secondaryKeys);
            return list.stream().sorted((a, b) -> meta.buildCacheKey(b).compareTo(meta.buildCacheKey(a))).toList();
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectListReverse", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    @Override
    public void update(Entity entity) {
        update(entity, false);
    }

    @Override
    public void update(Entity entity, boolean persistNow) {
        long primaryRouteId = entity.getPrimaryRouteId();
        try {
            if (!modelRouteChecker.checkWrite(primaryRouteId, meta)) {
                return;
            }
            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity cached = memoryCache.get(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (meta.hasJVM() && cached == null) {
                LogTopic.MODEL.error("updateNotFound", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            if (cached != entity) {
                LogTopic.MODEL.error("updateRefMismatch", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            entity.marshal();
            String cacheKey = meta.buildCacheKey(entity);
            // 立马持久化
            if (persistNow) {
                syncFlush(cacheKey, primaryRouteId, PersistOp.UPDATE);
                return;
            }
            // 异步刷新
            scheduleFlush(cacheKey, primaryRouteId, PersistOp.UPDATE);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "update", "table", meta.getTableName(), "routeId", primaryRouteId, "persistNow", persistNow);
        }
    }

    @Override
    public void insert(Entity entity) {
        insert(entity, false);
    }

    @Override
    public void insert(Entity entity, boolean persistNow) {
        long primaryRouteId = entity.getPrimaryRouteId();
        try {
            if (!modelRouteChecker.checkWrite(primaryRouteId, meta)) {
                return;
            }
            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity exists = memoryCache.get(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (exists != null) {
                LogTopic.MODEL.error("insertExists", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            memoryCache.put(entity);
            String cacheKey = meta.buildCacheKey(entity);
            if (persistNow) {
                syncFlush(cacheKey, primaryRouteId, PersistOp.INSERT);
                return;
            }
            // 异步刷新
            scheduleFlush(cacheKey, primaryRouteId, PersistOp.INSERT);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "insert", "table", meta.getTableName(), "routeId", primaryRouteId, "persistNow", persistNow);
        }
    }

    @Override
    public void insertOrUpdate(Entity entity) {
        long primaryRouteId = entity.getPrimaryRouteId();
        try {
            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity exists = memoryCache.get(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (exists == null && meta.hasRedis()) {
                exists = castEntity(redisModelCache.getOne(primaryRouteId, meta, meta.getPrimaryIndex(), keys));
                if (exists != null) {
                    memoryCache.put(exists);
                }
            }
            if (exists == null && meta.hasDb()) {
                exists = loadAndCacheFromDb(meta.getPrimaryIndex(), keys);
            }

            if (exists == null) {
                insert(entity);
            } else if (exists != entity) {
                LogTopic.MODEL.error("insertOrUpdateRefMismatch", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
            } else {
                update(entity);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "insertOrUpdate", "table", meta.getTableName(), "routeId", primaryRouteId);
        }
    }

    @Override
    public void delete(Entity entity) {
        delete(entity, false);
    }

    @Override
    public void delete(Entity entity, boolean persistNow) {
        long primaryRouteId = entity.getPrimaryRouteId();
        try {
            if (!modelRouteChecker.checkWrite(primaryRouteId, meta)) {
                return;
            }

            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity cached = memoryCache.get(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (cached != null && cached != entity) {
                LogTopic.MODEL.error("deleteRefMismatch", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            memoryCache.remove(primaryRouteId, meta.getPrimaryIndex(), keys);
            String cacheKey = meta.buildCacheKey(entity);
            if (persistNow) {
                syncFlush(cacheKey, primaryRouteId, PersistOp.DELETE);
                return;
            }
            // 异步刷新
            scheduleFlush(cacheKey, primaryRouteId, PersistOp.DELETE);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "delete", "table", meta.getTableName(), "routeId", primaryRouteId, "persistNow", persistNow);
        }
    }

    @Override
    public void deleteOne(Object... primaryKeys) {
        deleteOne(false, primaryKeys);
    }

    @Override
    public void deleteOne(boolean persistNow, Object... primaryKeys) {
        long primaryRouteId = 0L;
        try {
            IndexMeta primaryIndex = meta.getPrimaryIndex();
            if (!primaryIndex.isFullKey(primaryKeys)) {
                throw new ModelArgException("deleteOne 需要完整索引键");
            }
            primaryRouteId = meta.getRouteId(primaryKeys);
            if (!modelRouteChecker.checkWrite(primaryRouteId, meta)) {
                return;
            }
            memoryCache.remove(primaryRouteId, primaryIndex, primaryKeys);
            String cacheKey = meta.buildCacheKey(primaryIndex, primaryKeys);
            if (persistNow) {
                syncFlush(cacheKey, primaryRouteId, PersistOp.DELETE);
                return;
            }
            // 异步刷新
            scheduleFlush(cacheKey, primaryRouteId, PersistOp.DELETE);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "deleteOne", "table", meta.getTableName(), "routeId", primaryRouteId, "persistNow", persistNow);
        }
    }

    @Override
    public void deleteAll(Object... primaryKeys) {
        deleteAll(false, primaryKeys);
    }

    @Override
    public void deleteAll(boolean persistNow, Object... primaryKeys) {
        long primaryRouteId = 0L;
        try {
            IndexMeta primaryIndex = meta.getPrimaryIndex();
            primaryRouteId = meta.getRouteId(primaryKeys);
            if (!modelRouteChecker.checkWrite(primaryRouteId, meta)) {
                return;
            }

            // 1. 清理 JVM，并去掉这些实体上残留的 dirty，避免后续又被 upsert 回去
            List<Entity> removeList = memoryCache.removeAll(primaryIndex, primaryKeys);
            for (Entity entity : removeList) {
                dirtyTracker.remove(meta.getTableName(), meta.buildCacheKey(entity));
            }

            // 2. 下层统一按前缀删（覆盖 JVM 未加载 / 仅部分加载的情况）
            if (persistNow) {
                doDeleteByPrefix(primaryIndex, primaryKeys);
            } else {
                scheduleDeleteByPrefix(primaryIndex, primaryRouteId, primaryKeys);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "deleteAll", "table", meta.getTableName(), "primaryRouteId", primaryRouteId, "persistNow", persistNow);
        }
    }

    /**
     * 异步按前缀删除 Redis/DB（不走残缺 cacheKey 的 dirty 队列）
     */
    private void scheduleDeleteByPrefix(IndexMeta primaryIndex, long primaryRouteId, Object... primaryKeys) {
        if (!meta.hasRedis() && !meta.hasDb()) {
            return;
        }
        String taskKey = "prefixDelete:" + meta.buildCacheKey(primaryIndex, primaryKeys);
        persistThreadPool.submit(primaryRouteId, meta.getTableName(), taskKey, () -> doDeleteByPrefix(primaryIndex, primaryKeys));
    }

    private void doDeleteByPrefix(IndexMeta primaryIndex, Object... primaryKeys) {
        try {
            if (meta.hasRedis()) {
                redisModelCache.deleteByPrefix(meta, primaryIndex, primaryKeys);
            }
            if (meta.hasDb()) {
                persistEngineFactory.getEngine(meta).deleteByPrefix(meta, primaryIndex, primaryKeys);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "deleteByPrefixFail", "table", meta.getTableName(), "primaryIndex", primaryIndex.getName());
        }
    }



    // ===============================【ModelHook】===============================

    @Override
    public void bindMeta(ModelMeta modelMeta) {
        this.meta = modelMeta;
        this.memoryCache = new MemoryModelCache<>(modelMeta);
        this.memoryCache.setEvictionListener(this::onEvicted);
        this.memoryCache.setRouteEvictionListener(this::onRouteBucketEvicted);
    }

    /**
     * 桶内 capacity LRU 淘汰：标记删除并异步落库
     */
    private void onEvicted(Entity entity) {
        try {
            String cacheKey = meta.buildCacheKey(entity);
            long routeId = entity.getPrimaryRouteId();
            // 异步刷新
            scheduleFlush(cacheKey, routeId, PersistOp.DELETE);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "onEvicted", "table", meta.getTableName());
        }
    }

    /**
     * 外层 route 桶因 SIZE/EXPIRED 被卸：在实体离开内存前 flush dirty（不能当 DELETE）
     */
    private void onRouteBucketEvicted(long routeId, Collection<Entity> entities) {
        try {
            Map<String, Entity> entityByCacheKey = new HashMap<>(entities.size());
            for (Entity entity : entities) {
                entityByCacheKey.put(meta.buildCacheKey(entity), entity);
            }
            for (DirtyEntry entry : dirtyTracker.listByTableAndRouteId(meta.getTableName(), routeId)) {
                try {
                    if (entry.getOp() == PersistOp.DELETE) {
                        flushDelete(entry.getCacheKey(), routeId);
                    } else {
                        Entity entity = entityByCacheKey.get(entry.getCacheKey());
                        if (entity == null) {
                            continue;
                        }
                        flushUpsertEntity(entity);
                    }
                    dirtyTracker.remove(meta.getTableName(), entry.getCacheKey());
                } catch (Exception e) {
                    LogTopic.MODEL.error(e, "routeBucketEvictFlush", "table", meta.getTableName(),
                        "routeId", routeId, "cacheKey", entry.getCacheKey());
                }
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "onRouteBucketEvicted", "table", meta.getTableName(), "routeId", routeId);
        }
    }

    /** 异步刷新到redis/db */
    private void scheduleFlush(String cacheKey, long routeId, PersistOp op) {
        if (!meta.hasRedis() && !meta.hasDb()) {
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, op);
        submitFlush(cacheKey, routeId);
    }

    private void submitFlush(String cacheKey, long routeId) {
        persistThreadPool.submit(routeId, meta.getTableName(), cacheKey, () -> {
            if (!dirtyTracker.isDirty(meta.getTableName(), cacheKey)) {
                return;
            }
            DirtyEntry entry = dirtyTracker.get(meta.getTableName(), cacheKey);
            if (entry == null) {
                return;
            }
            doFlush(cacheKey, routeId, entry.getOp());
            if (dirtyTracker.isDirty(meta.getTableName(), cacheKey)) {
                submitFlush(cacheKey, routeId);
            }
        });
    }

    @Override
    public void flushRoute(long routeId) {
        for (DirtyEntry entry : dirtyTracker.listByTableAndRouteId(meta.getTableName(), routeId)) {
            doFlush(entry.getCacheKey(), routeId, entry.getOp());
        }
    }

    /**
     * 清理指定 routeId 的 JVM 缓存
     */
    @Override
    public void clearRouteCache(long routeId) {
        memoryCache.clearRoute(routeId);
    }

    /**
     * flush 当前表全部脏数据（关服使用）
     */
    @Override
    public void flushAllDirty() {
        for (DirtyEntry entry : dirtyTracker.listByTable(meta.getTableName())) {
            doFlush(entry.getCacheKey(), entry.getRouteId(), entry.getOp());
        }
    }

    /**
     * 预加载分片数据（L2 Redis → L1 JVM；Redis 无数据时从 DB 加载并回填）
     */
    @Override
    public void preload(long primaryRouteId) {
        if (!meta.isPreLoad()) {
            return;
        }
        if (meta.hasRedis()) {
            Map<String, BaseEntity> fieldEntityMap = redisModelCache.loadRouteBucket(meta, entityClass(), primaryRouteId);
            memoryCache.putAll(primaryRouteId, fieldEntityMap);
            if (!fieldEntityMap.isEmpty()) {
                return;
            }
        }
        if (meta.hasDb()) {
            List<Entity> dbList = loadAndCacheListFromDb(meta.getPrimaryIndex(), primaryRouteId);
            LogTopic.MODEL.info("preloadFromDb", "table", meta.getTableName(), "primaryRouteId", primaryRouteId, "size", dbList.size());
        }
    }

    /**
     * 定时器重试调用
     */
    @Override
    public void retryFlush(String cacheKey) {
        if (!dirtyTracker.isDirty(meta.getTableName(), cacheKey)) {
            return;
        }
        DirtyEntry entry = dirtyTracker.get(meta.getTableName(), cacheKey);
        if (entry == null) {
            return;
        }
        doFlush(cacheKey, entry.getRouteId(), entry.getOp());
    }

    private void syncFlush(String cacheKey, long routeId, PersistOp op) {
        doFlush(cacheKey, routeId, op);
    }

    private void doFlush(String cacheKey, long routeId, PersistOp op) {
        try {
            if (op == PersistOp.DELETE) {
                flushDelete(cacheKey, routeId);
            } else {
                flushUpsert(cacheKey, routeId);
            }
            dirtyTracker.remove(meta.getTableName(), cacheKey);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "flushFail", "table", meta.getTableName(), "cacheKey", cacheKey, "op", op);
        }
    }

    private void flushDelete(String cacheKey, long routeId) {
        IndexMeta index = meta.getPrimaryIndex();
        Object[] keys = parseKeysFromCacheKey(cacheKey, index);
        if (meta.hasRedis()) {
            redisModelCache.delete(routeId, meta, index, keys);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).delete(meta, index, keys);
        }
    }

    private Object[] parseKeysFromCacheKey(String cacheKey, IndexMeta index) {
        String prefix = meta.getTableName() + ':' + index.getName() + ':';
        if (!cacheKey.startsWith(prefix)) {
            throw new ModelArgException("非法 cacheKey: " + cacheKey);
        }
        String keyPart = cacheKey.substring(prefix.length());
        if (keyPart.isEmpty()) {
            throw new ModelArgException("非法 cacheKey: " + cacheKey);
        }
        String[] parts = keyPart.split(":");
        Object[] keys = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            keys[i] = parseValue(parts[i]);
        }
        return keys;
    }

    private Object parseValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void flushUpsert(String cacheKey, long routeId) {
        Entity entity = findEntityByCacheKey(cacheKey, routeId);
        if (entity == null) {
            return;
        }
        flushUpsertEntity(entity);
    }

    private void flushUpsertEntity(Entity entity) {
        if (meta.hasRedis()) {
            redisModelCache.save(meta, entity);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).upsert(meta, entity);
        }
    }

    private Entity findEntityByCacheKey(String cacheKey, long routeId) {
        List<Entity> bucket = memoryCache.getList(routeId, meta.getPrimaryIndex(), routeId);
        for (Entity entity : bucket) {
            if (cacheKey.equals(meta.buildCacheKey(entity))) {
                return entity;
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    protected Class<Entity> entityClass() {
        return (Class<Entity>) meta.getEntityClass();
    }

    /** 转为业务 Entity */
    @SuppressWarnings("unchecked")
    private Entity castEntity(BaseEntity entity) {
        return (Entity) entity;
    }

    /** 转为业务 Entity 列表 */
    @SuppressWarnings("unchecked")
    private List<Entity> castList(List<? extends BaseEntity> list) {
        return (List<Entity>) list;
    }

}
