package com.mumu.game.core.db.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private RedisModelCache redisModelCache;
    @Autowired
    private DirtyTracker dirtyTracker;
    @Autowired
    private PersistThreadPool persistThreadPool;
    @Autowired
    private PersistEngineFactory persistEngineFactory;
    @Autowired
    private ModelRouteChecker modelRouteChecker;


    @Override
    public Entity selectOne(Object... primaryKeys) {
        return selectOne(meta.getPrimaryIndex().getName(), primaryKeys);
    }

    @Override
    public Entity selectOne(String indexName, Object... primaryKeys) {
        IndexMeta index = meta.getIndex(indexName);
        if (!index.isFullKey(primaryKeys)) {
            throw new IllegalArgumentException("selectOne 需要完整索引键");
        }
        Entity local = jvmCache.get(index, primaryKeys);
        if (local != null) {
            return local;
        }
        if (meta.hasRedis()) {
            Entity redis = redisModelCache.getOne(meta, index, domainClass(), primaryKeys);
            if (redis != null) {
                jvmCache.put(redis);
                return redis;
            }
        }
        return loadOneFromDb(index, primaryKeys);
    }

    private Entity loadOneFromDb(IndexMeta index, Object... keys) {
        if (!meta.hasDb()) {
            return null;
        }
        Entity db = persistEngineFactory.getEngine(meta).findOne(meta, index, domainClass(), keys);
        if (db != null) {
            cacheEntity(db);
        }
        return db;
    }

    /**
     * DB/Redis 回填：写入 L1，并同步回填 L2
     */
    private void cacheEntity(Entity entity) {
        entity.marshal();
        jvmCache.put(entity);
        saveToRedisQuietly(entity);
    }

    private void saveToRedisQuietly(Entity entity) {
        if (!meta.hasRedis()) {
            return;
        }
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
    public List<Entity> selectList(String indexName, Object... primaryKeys) {
        IndexMeta index = meta.getIndex(indexName);
        if (index.isFullKey(primaryKeys)) {
            Entity one = selectOne(indexName, primaryKeys);
            return one == null ? Collections.emptyList() : List.of(one);
        }
        List<Entity> local = jvmCache.list(index, primaryKeys);
        if (!local.isEmpty()) {
            return local;
        }
        if (meta.hasRedis()) {
            List<Entity> redisList = redisModelCache.getList(meta, index, domainClass(), primaryKeys);
            if (!redisList.isEmpty()) {
                for (Entity entity : redisList) {
                    jvmCache.put(entity);
                }
                return redisList;
            }
        }
        return loadListFromDb(index, primaryKeys);
    }

    private List<Entity> loadListFromDb(IndexMeta index, Object... keys) {
        if (!meta.hasDb()) {
            return Collections.emptyList();
        }
        List<Entity> dbList = persistEngineFactory.getEngine(meta).findList(meta, index, domainClass(), keys);
        if (!dbList.isEmpty()) {
            cacheEntities(dbList);
        }
        return dbList;
    }

    private void cacheEntities(List<Entity> entities) {
        for (Entity entity : entities) {
            cacheEntity(entity);
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
        long routeId = meta.getRouteId(entity);
        modelRouteChecker.checkWrite(routeId, meta);
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity cached = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (cached == null) {
            throw new IllegalStateException("update 失败，记录不存在: " + meta.buildCacheKey(entity));
        }
        if (cached != entity) {
            throw new IllegalStateException("update 失败，必须使用 JVM 缓存中的同一对象引用: " + meta.buildCacheKey(entity));
        }
        entity.marshal();
        String cacheKey = meta.buildCacheKey(entity);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.UPDATE);
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.UPDATE);
        scheduleFlush(cacheKey, routeId);
    }

    @Override
    public void insert(Entity entity) {
        insert(entity, false);
    }

    @Override
    public void insert(Entity entity, boolean persistNow) {
        long routeId = meta.getRouteId(entity);
        modelRouteChecker.checkWrite(routeId, meta);
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity exists = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (exists != null) {
            throw new IllegalStateException("insert 失败，记录已存在: " + meta.buildCacheKey(entity));
        }
        entity.marshal();
        jvmCache.put(entity);
        String cacheKey = meta.buildCacheKey(entity);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.INSERT);
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.INSERT);
        scheduleFlush(cacheKey, routeId);
    }

    @Override
    public void insertOrUpdate(Entity entity) {
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity exists = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (exists == null && meta.hasRedis()) {
            exists = redisModelCache.getOne(meta, meta.getPrimaryIndex(), domainClass(), keys);
            if (exists != null) {
                jvmCache.put(exists);
            }
        }
        if (exists == null && meta.hasDb()) {
            exists = loadOneFromDb(meta.getPrimaryIndex(), keys);
        }
        if (exists == null) {
            insert(entity);
        } else if (exists != entity) {
            throw new IllegalStateException(
                "insertOrUpdate 失败，update 必须使用 JVM 缓存中的同一对象引用: " + meta.buildCacheKey(entity));
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
        long routeId = meta.getRouteId(entity);
        modelRouteChecker.checkWrite(routeId, meta);
        Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
        Entity cached = jvmCache.get(meta.getPrimaryIndex(), keys);
        if (cached != null && cached != entity) {
            throw new IllegalStateException(
                "delete 失败，必须使用 JVM 缓存中的同一对象引用: " + meta.buildCacheKey(entity));
        }
        jvmCache.remove(meta.getPrimaryIndex(), keys);
        String cacheKey = meta.buildCacheKey(entity);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.DELETE);
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.DELETE);
        scheduleFlush(cacheKey, routeId);
    }

    @Override
    public void deleteOne(Object... primaryKeys) {
        deleteOne(false, primaryKeys);
    }

    @Override
    public void deleteOne(boolean persistNow, Object... primaryKeys) {
        deleteOne(meta.getPrimaryIndex().getName(), persistNow, primaryKeys);
    }

    @Override
    public void deleteOne(String indexName, boolean persistNow, Object... primaryKeys) {
        IndexMeta index = meta.getIndex(indexName);
        if (!index.isFullKey(primaryKeys)) {
            throw new IllegalArgumentException("deleteOne 需要完整索引键");
        }
        long routeId = meta.getRouteId(primaryKeys);
        modelRouteChecker.checkWrite(routeId, meta);
        jvmCache.remove(index, primaryKeys);
        String cacheKey = meta.buildCacheKey(index, primaryKeys);
        if (persistNow) {
            syncFlush(cacheKey, routeId, PersistOp.DELETE);
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.DELETE);
        scheduleFlush(cacheKey, routeId);
    }

    @Override
    public void deleteAll(Object... primaryKeys) {
        deleteAll(meta.getPrimaryIndex().getName(), false, primaryKeys);
    }

    @Override
    public void deleteAll(String indexName, Object... primaryKeys) {
        deleteAll(indexName, false, primaryKeys);
    }

    @Override
    public void deleteAll(String indexName, boolean persistNow, Object... primaryKeys) {
        IndexMeta index = meta.getIndex(indexName);
        long routeId = meta.getRouteId(primaryKeys);
        modelRouteChecker.checkWrite(routeId, meta);
        List<Entity> removed = jvmCache.removeAll(index, primaryKeys);
        for (Entity entity : removed) {
            String cacheKey = meta.buildCacheKey(entity);
            if (persistNow) {
                syncFlush(cacheKey, routeId, PersistOp.DELETE);
            } else {
                dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.DELETE);
                scheduleFlush(cacheKey, routeId);
            }
        }
        if (!persistNow && removed.isEmpty()) {
            String cacheKey = meta.buildCacheKey(index, primaryKeys);
            dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.DELETE);
            scheduleFlush(cacheKey, routeId);
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
        long routeId = meta.getRouteId(entity);
        dirtyTracker.mark(meta.getTableName(), cacheKey, routeId, PersistOp.DELETE);
        scheduleFlush(cacheKey, routeId);
    }

    private void scheduleFlush(String cacheKey, long routeId) {
        if (!meta.hasRedis() && !meta.hasDb()) {
            dirtyTracker.remove(meta.getTableName(), cacheKey);
            return;
        }
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
                scheduleFlush(cacheKey, routeId);
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
    public void preload(long routeId) {
        if (!meta.isPreLoad()) {
            return;
        }
        if (meta.hasRedis()) {
            Map<String, Entity> bucket = redisModelCache.loadRouteBucket(meta, domainClass(), routeId);
            jvmCache.putAll(routeId, bucket);
            if (!bucket.isEmpty()) {
                return;
            }
        }
        if (meta.hasDb()) {
            List<Entity> dbList = loadListFromDb(meta.getPrimaryIndex(), routeId);
            LogTopic.MODEL.info("preloadFromDb", "table", meta.getTableName(), "routeId", routeId, "size", dbList.size());
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
    protected Class<Entity> domainClass() {
        return (Class<Entity>) meta.getEntityClass();
    }

}
