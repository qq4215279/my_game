package com.mumu.game.core.db.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.cache.JvmModelCache;
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
    /** 缓存 */
    private JvmModelCache<Entity> jvmCache;

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
        return selectOne(meta.getPrimaryIndex().getName(), primaryKeys);
    }

    @Override
    public Entity selectOne(String indexName, Object... keys) {
        IndexMeta index = meta.getIndex(indexName);
        if (!index.isFullKey(keys)) {
            throw new IllegalArgumentException("selectOne 需要完整索引键");
        }

        // 1. jvm查询
        Entity local = jvmCache.get(index, keys);
        if (local != null) {
            return local;
        }
        // 2. redis查询
        if (meta.hasRedis()) {
            Entity redis = redisModelCache.getOne(meta, index, entityClass(), keys);
            if (redis != null) {
                jvmCache.put(redis);
                return redis;
            }
        }
        // 3. db查询
        if (meta.hasDb()) {
            return loadAndCacheFromDb(index,  meta.getRouteId(keys));
        }

        return null;
    }

    private Entity loadAndCacheFromDb(IndexMeta index, Object... keys) {
        if (!meta.hasDb()) {
            return null;
        }
        Entity db = persistEngineFactory.getEngine(meta).findOne(meta, index, entityClass(), keys);
        if (db != null) {
            cacheEntity(db);
        }
        return db;
    }

    /**
     * DB/Redis 回填：写入 L1，并同步回填 L2
     */
    private void cacheEntity(Entity entity) {
        if (meta.hasJVM()) {
            jvmCache.put(entity);
        }
        saveToRedisQuietly(entity);
    }

    private void saveToRedisQuietly(Entity entity) {
        if (!meta.hasRedis()) {
            return;
        }
        // TODO 是否优化采用异步save？
        try {
            redisModelCache.save(meta, entity);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "cacheBackfillRedisSave", "table", meta.getTableName());
        }
    }

    @Override
    public List<Entity> selectList(Object... primaryKeys) {
        return selectList(meta.getPrimaryIndex().getName(), primaryKeys);
    }

    @Override
    public List<Entity> selectList(String indexName, Object... keys) {
        IndexMeta index = meta.getIndex(indexName);
        if (index.isFullKey(keys)) {
            Entity one = selectOne(indexName, keys);
            return one == null ? Collections.emptyList() : List.of(one);
        }

        // 1. jvm查询
        if (meta.hasJVM()) {
            List<Entity> local = jvmCache.list(index, keys);
            if (!local.isEmpty()) {
                return local;
            }
        }
        // 2. redis查询
        if (meta.hasRedis()) {
            List<Entity> redisList = redisModelCache.getList(meta, index, entityClass(), keys);
            // 缓存到jvm
            cache2Jvm(redisList);
            return redisList;
        }
        // 3. db查询
        if (meta.hasDb()) {
            return loadAndCacheListFromDb(index,  meta.getRouteId(keys));
        }

        return Collections.emptyList();
    }

    private List<Entity> loadAndCacheListFromDb(IndexMeta index, long primaryRouteId) {
        if (!meta.hasDb()) {
            return Collections.emptyList();
        }
        // 非主索引
        if (index.isPrimary()) {
            LogTopic.MODEL.error("loadListFromDb", "非主索引无法从db中查询", "table", meta.getTableName(), "index",
                    index.getName());
            return Collections.emptyList();
        }

        List<Entity> dbList = persistEngineFactory.getEngine(meta).findList(meta, index, entityClass(), primaryRouteId);
        if (!dbList.isEmpty()) {
            // 缓存到jvm
            cache2Jvm(dbList);
            // 缓存到redis
            cache2Redis(dbList);
        }
        return dbList;
    }

    /** 缓存到jvm */
    private void cache2Jvm(List<Entity> entities) {
        // 缓存到jvm
        if (meta.hasJVM()) {
            for (Entity entity : entities) {
                jvmCache.put(entity);
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
        return selectListReverse(meta.getPrimaryIndex().getName(), primaryKeys);
    }

    @Override
    public List<Entity> selectListReverse(String indexName, Object... primaryKeys) {
        IndexMeta index = meta.getIndex(indexName);
        if (index.isFullKey(primaryKeys)) {
            Entity one = selectOne(indexName, primaryKeys);
            return one == null ? Collections.emptyList() : List.of(one);
        }
        List<Entity> local = jvmCache.listReverse(index, primaryKeys);
        if (!local.isEmpty()) {
            return local;
        }
        List<Entity> list = selectList(indexName, primaryKeys);
        return list.stream().sorted((a, b) -> meta.buildCacheKey(b).compareTo(meta.buildCacheKey(a))).toList();
    }

    @Override
    public void update(Entity entity) {
        update(entity, false);
    }

    @Override
    public void update(Entity entity, boolean persistNow) {
        long routeId = entity.getPrimaryRouteId();
        if (!modelRouteChecker.checkWrite(routeId, meta)) {
            return;
        }
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity cached = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (meta.hasJVM() && cached == null) {
            LogTopic.MODEL.error("updateNotFound", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
            return;
        }
        if (cached != entity) {
            LogTopic.MODEL.error("updateRefMismatch", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
            return;
        }
        entity.marshal();
        String cacheKey = meta.buildCacheKey(entity);
        // 立马持久化
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.UPDATE);
            return;
        }
        scheduleFlush(cacheKey, routeId, PersistOp.UPDATE);
    }

    @Override
    public void insert(Entity entity) {
        insert(entity, false);
    }

    @Override
    public void insert(Entity entity, boolean persistNow) {
        long routeId = entity.getPrimaryRouteId();
        if (!modelRouteChecker.checkWrite(routeId, meta)) {
            return;
        }
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity exists = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (exists != null) {
            LogTopic.MODEL.error("insertExists", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
            return;
        }
        jvmCache.put(entity);
        String cacheKey = meta.buildCacheKey(entity);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.INSERT);
            return;
        }
        scheduleFlush(cacheKey, routeId, PersistOp.INSERT);
    }

    @Override
    public void insertOrUpdate(Entity entity) {
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity exists = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (exists == null && meta.hasRedis()) {
            exists = redisModelCache.getOne(meta, meta.getPrimaryIndex(), entityClass(), keys);
            if (exists != null) {
                jvmCache.put(exists);
            }
        }
        if (exists == null && meta.hasDb()) {
            exists = loadAndCacheFromDb(meta.getPrimaryIndex(), keys);
        }

        if (exists == null) {
            insert(entity);
        } else if (exists != entity) {
            LogTopic.MODEL.error("insertOrUpdateRefMismatch", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
        } else {
            update(entity);
        }
    }

    @Override
    public void delete(Entity entity) {
        delete(entity, false);
    }

    @Override
    public void delete(Entity entity, boolean persistNow) {
        long routeId = entity.getPrimaryRouteId();
        if (!modelRouteChecker.checkWrite(routeId, meta)) {
            return;
        }
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity cached = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (cached != null && cached != entity) {
            LogTopic.MODEL.error("deleteRefMismatch", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
            return;
        }
        jvmCache.remove(meta.getPrimaryIndex(), keys);
        String cacheKey = meta.buildCacheKey(entity);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.DELETE);
            return;
        }
        scheduleFlush(cacheKey, routeId, PersistOp.DELETE);
    }

    @Override
    public void deleteOne(Object... primaryKeys) {
        deleteOne(false, primaryKeys);
    }

    @Override
    public void deleteOne(boolean persistNow, Object... primaryKeys) {
        IndexMeta primaryIndex = meta.getPrimaryIndex();
        if (!primaryIndex.isFullKey(primaryKeys)) {
            throw new IllegalArgumentException("deleteOne 需要完整索引键");
        }
        long routeId = meta.getRouteId(primaryKeys);
        if (!modelRouteChecker.checkWrite(routeId, meta)) {
            return;
        }
        jvmCache.remove(primaryIndex, primaryKeys);
        String cacheKey = meta.buildCacheKey(primaryIndex, primaryKeys);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.DELETE);
            return;
        }
        scheduleFlush(cacheKey, routeId, PersistOp.DELETE);
    }

    @Override
    public void deleteAll(Object... primaryKeys) {
        deleteAll(false, primaryKeys);
    }

    @Override
    public void deleteAll(boolean persistNow, Object... primaryKeys) {
        IndexMeta primaryIndex = meta.getPrimaryIndex();
        long routeId = meta.getRouteId(primaryKeys);
        if (!modelRouteChecker.checkWrite(routeId, meta)) {
            return;
        }

        // 1. 清理 JVM，并去掉这些实体上残留的 dirty，避免后续又被 upsert 回去
        List<Entity> removeList = jvmCache.removeAll(primaryIndex, primaryKeys);
        for (Entity entity : removeList) {
            dirtyTracker.remove(meta.getTableName(), meta.buildCacheKey(entity));
        }

        // 2. 下层统一按前缀删（覆盖 JVM 未加载 / 仅部分加载的情况）
        if (persistNow) {
            doDeleteByPrefix(primaryIndex, primaryKeys);
        } else {
            scheduleDeleteByPrefix(primaryIndex, routeId, primaryKeys);
        }
    }

    /**
     * 异步按前缀删除 Redis/DB（不走残缺 cacheKey 的 dirty 队列）
     */
    private void scheduleDeleteByPrefix(IndexMeta primaryIndex, long routeId, Object... primaryKeys) {
        if (!meta.hasRedis() && !meta.hasDb()) {
            return;
        }
        String taskKey = "prefixDelete:" + meta.buildCacheKey(primaryIndex, primaryKeys);
        persistThreadPool.submit(routeId, meta.getTableName(), taskKey, () -> doDeleteByPrefix(primaryIndex, primaryKeys));
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
        this.jvmCache = new JvmModelCache<>(modelMeta);
        this.jvmCache.setEvictionListener(this::onEvicted);
    }

    /**
     * LRU 淘汰时标记删除并异步落库
     */
    private void onEvicted(Entity entity) {
        String cacheKey = meta.buildCacheKey(entity);
        long routeId = entity.getPrimaryRouteId();
        scheduleFlush(cacheKey, routeId, PersistOp.DELETE);
    }

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
        jvmCache.clearRoute(routeId);
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
            Map<String, Entity> bucket = redisModelCache.loadRouteBucket(meta, entityClass(), primaryRouteId);
            jvmCache.putAll(primaryRouteId, bucket);
            if (!bucket.isEmpty()) {
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
            redisModelCache.delete(meta, index, keys);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).delete(meta, index, keys);
        }
    }

    private Object[] parseKeysFromCacheKey(String cacheKey, IndexMeta index) {
        String prefix = meta.getTableName() + ':' + index.getName() + ':';
        if (!cacheKey.startsWith(prefix)) {
            throw new IllegalArgumentException("非法 cacheKey: " + cacheKey);
        }
        String keyPart = cacheKey.substring(prefix.length());
        if (keyPart.isEmpty()) {
            throw new IllegalArgumentException("非法 cacheKey: " + cacheKey);
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
        if (meta.hasRedis()) {
            redisModelCache.save(meta, entity);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).upsert(meta, entity);
        }
    }

    private Entity findEntityByCacheKey(String cacheKey, long routeId) {
        List<Entity> bucket = jvmCache.list(meta.getPrimaryIndex(), routeId);
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

}
