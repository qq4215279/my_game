package com.mumu.game.core.db.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.core.cache.MemoryModelCache;
import com.mumu.game.core.db.core.cache.RedisModelCache;
import com.mumu.game.core.db.consts.PersistOp;
import com.mumu.game.core.db.core.dirty.DirtyEntry;
import com.mumu.game.core.db.core.dirty.DirtyTracker;
import com.mumu.game.core.db.core.persist.PersistEngineFactory;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.lifecycle.PersistThreadPool;
import com.mumu.game.core.db.util.EntitySerializer;
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
            // 仅 DB：无 JVM/Redis，每次直查引擎，不回填缓存
            if (!meta.hasJVM()) {
                return selectOneFromDb(index, secondaryKeys);
            }
            // 有 JVM：未加载则按 route 整桶灌入并标记已加载（可为空），再查内存
            ensureRouteLoaded(primaryRouteId);
            return memoryCache.getOne(primaryRouteId, index, secondaryKeys);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectOne", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return null;
        }
    }

    /** 仅 DB：按完整索引键查单条，不回填 JVM/Redis */
    private Entity selectOneFromDb(IndexMeta index, Object... keys) {
        if (!meta.hasDb()) {
            return null;
        }
        return persistEngineFactory.getEngine(meta).findOne(meta, index, keys);
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
            if (!meta.hasJVM()) {
                return selectListFromDb(index, secondaryKeys);
            }
            ensureRouteLoaded(primaryRouteId);
            return castList(memoryCache.getList(primaryRouteId, index, secondaryKeys));
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectList", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return Collections.emptyList();
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
            if (!meta.hasJVM()) {
                List<Entity> list = selectListFromDb(index, secondaryKeys);
                return list.stream()
                    .sorted((a, b) -> meta.buildCacheKey(b).compareTo(meta.buildCacheKey(a)))
                    .toList();
            }
            ensureRouteLoaded(primaryRouteId);
            return castList(memoryCache.getListReverse(primaryRouteId, index, secondaryKeys));
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "selectListReverse", "table", meta.getTableName(),
                "index", indexName, "routeId", primaryRouteId);
            return Collections.emptyList();
        }
    }

    /** 仅 DB：按索引左前缀查列表，不回填 JVM/Redis */
    private List<Entity> selectListFromDb(IndexMeta index, Object... keys) {
        if (!meta.hasDb()) {
            return Collections.emptyList();
        }
        List<Entity> list = persistEngineFactory.getEngine(meta).findList(meta, index, keys);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 确保 primaryRouteId 分片已加载到 JVM（仅 {@code hasJVM()=true} 的策略）。
     * 若尚未加载：Redis 整桶（或单字段精确 field）→ 空则 DB {@code findList(routeId)} → 回填；
     * 无论有无数据都 {@link MemoryModelCache#markRouteLoaded}，空桶表示「已加载但无数据」，后续不再穿透。
     * {@link com.mumu.game.core.db.consts.PersistStrategy#DB} 无 JVM，本方法直接返回。
     * 加载失败不打标，允许下次重试。
     */
    private void ensureRouteLoaded(long primaryRouteId) {
        // 仅 DB：无缓存层，不预加载、不灌桶
        if (!meta.hasJVM()) {
            return;
        }
        // 已加载过数据
        if (memoryCache.isRouteLoaded(primaryRouteId)) {
            return;
        }
        // 纯 JVM 策略：无下层存储，直接标记（空）
        if (!meta.hasRedis() && !meta.hasDb()) {
            memoryCache.markRouteLoaded(primaryRouteId);
            return;
        }

        try {
            boolean filled = false;
            // 从redis加载
            if (meta.hasRedis()) {
                Map<String, BaseEntity> fieldEntityMap = redisModelCache.loadRouteBucket(meta, entityClass(), primaryRouteId);
                if (!fieldEntityMap.isEmpty()) {
                    memoryCache.saveBatch(meta, List.copyOf(fieldEntityMap.values()));
                    filled = true;
                }
            }
            // redis没有数据，再从db加载
            if (!filled && meta.hasDb()) {
                List<Entity> dbList = persistEngineFactory.getEngine(meta)
                    .findList(meta, meta.getPrimaryIndex(), primaryRouteId);
                if (!dbList.isEmpty()) {
                    memoryCache.saveBatch(meta, dbList);
                    cache2Redis(dbList);
                }
            }
            // 有数据时 saveBatch 已建桶；无数据时建空桶作为已加载标记
            memoryCache.markRouteLoaded(primaryRouteId);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "ensureRouteLoaded", "table", meta.getTableName(), "routeId", primaryRouteId);
        }
    }

    /** 缓存到redis */
    private void cache2Redis(List<Entity> entities) {
        if (!meta.hasRedis() || entities == null || entities.isEmpty()) {
            return;
        }
        try {
            redisModelCache.saveBatch(meta, entities);
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "cache2Redis saveBatch error", "table", meta.getTableName());
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
            // 仅 DB：无 JVM；persistNow 同步，否则 dirty 快照异步
            if (!meta.hasJVM()) {
                if (persistNow) {
                    saveEntityOnlyDb(entity);
                } else {
                    scheduleSaveOnlyDb(entity, PersistOp.UPDATE);
                }
                return;
            }

            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity cached = memoryCache.getOne(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (cached == null) {
                LogTopic.MODEL.error("updateNotFound", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            if (cached != entity) {
                LogTopic.MODEL.error("updateRefMismatch", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            entity.unmarshal();
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

    /** 仅 DB：同步写入持久引擎 */
    private void saveEntityOnlyDb(Entity entity) {
        persistEngineFactory.getEngine(meta).upsert(meta, entity);
    }

    /** 仅 DB：异步 upsert（dirty 带快照） */
    private void scheduleSaveOnlyDb(Entity entity, PersistOp op) {
        String cacheKey = meta.buildCacheKey(entity);
        long primaryRouteId = entity.getPrimaryRouteId();
        Entity snapshot = snapshot4DirtyOnlyDb(entity);
        dirtyTracker.mark(meta.getTableName(), cacheKey, primaryRouteId, op, snapshot, null);
        doSubmitScheduleTask(cacheKey, primaryRouteId);
    }

    /**
     * 仅 DB：为异步 flush 生成实体快照（序列化拷贝，避免业务后续改同一引用污染未刷出任务）
     */
    @SuppressWarnings("unchecked")
    private Entity snapshot4DirtyOnlyDb(Entity entity) {
        entity.unmarshal();
        String json = EntitySerializer.serialize(meta, entity);
        Entity copy = EntitySerializer.deserialize(meta, json, entityClass());
        if (copy != null) {
            copy.unmarshal();
            return copy;
        }
        LogTopic.MODEL.error("snapshotForDirtyFail", "table", meta.getTableName(), "cacheKey", meta.buildCacheKey(entity));
        return entity;
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
            // 仅 DB：persistNow 同步，否则 dirty 快照异步
            if (!meta.hasJVM()) {
                if (persistNow) {
                    saveEntityOnlyDb(entity);
                } else {
                    scheduleSaveOnlyDb(entity, PersistOp.INSERT);
                }
                return;
            }

            Object[] keys = meta.getPrimaryIndex().readKeyValues(entity);
            Entity exists = memoryCache.getOne(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (exists != null) {
                LogTopic.MODEL.error("insertExists", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            memoryCache.save(meta, entity);
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
            // 仅 DB：无引用语义，按 upsert 异步/同步写库（避免先 findOne 的竞态）
            if (!meta.hasJVM()) {
                update(entity, false);
                return;
            }
            ensureRouteLoaded(primaryRouteId);
            Entity exists = memoryCache.getOne(primaryRouteId, meta.getPrimaryIndex(), keys);

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
            // 仅 DB：persistNow 同步删，否则 dirty 异步删
            if (!meta.hasJVM()) {
                if (persistNow) {
                    deleteEntityOnlyDb(meta.getPrimaryIndex(), keys);
                } else {
                    scheduleDeleteOnlyDb(primaryRouteId, keys);
                }
                return;
            }

            Entity cached = memoryCache.getOne(primaryRouteId, meta.getPrimaryIndex(), keys);
            if (cached != null && cached != entity) {
                LogTopic.MODEL.error("deleteRefMismatch", "table", meta.getTableName(), "routeId", primaryRouteId, "cacheKey", meta.buildCacheKey(entity));
                return;
            }
            memoryCache.delete(primaryRouteId, meta.getPrimaryIndex(), keys);
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

    /** 仅 DB：按主键同步删除 */
    private void deleteEntityOnlyDb(IndexMeta index, Object... keys) {
        persistEngineFactory.getEngine(meta).delete(meta, index, keys);
    }

    /** 仅 DB：异步按主键删除（dirty 带删除键） */
    private void scheduleDeleteOnlyDb(long primaryRouteId, Object... keys) {
        IndexMeta index = meta.getPrimaryIndex();
        String cacheKey = meta.buildCacheKey(index, keys);
        Object[] keyCopy = keys.clone();
        dirtyTracker.mark(meta.getTableName(), cacheKey, primaryRouteId, PersistOp.DELETE, null, keyCopy);
        doSubmitScheduleTask(cacheKey, primaryRouteId);
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
            if (!meta.hasJVM()) {
                if (persistNow) {
                    deleteEntityOnlyDb(primaryIndex, primaryKeys);
                } else {
                    scheduleDeleteOnlyDb(primaryRouteId, primaryKeys);
                }
                return;
            }
            memoryCache.delete(primaryRouteId, primaryIndex, primaryKeys);
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

            // 仅 DB：无 JVM，按前缀删库（同步或异步）
            if (!meta.hasJVM()) {
                if (!meta.hasDb()) {
                    return;
                }
                if (persistNow) {
                    persistEngineFactory.getEngine(meta).deleteByPrefix(meta, primaryIndex, primaryKeys);
                } else {
                    scheduleDeleteByPrefix(primaryIndex, primaryRouteId, primaryKeys);
                }
                return;
            }

            // 1. 清理 JVM，并去掉这些实体上残留的 dirty，避免后续又被 upsert 回去
            List<Entity> removeList = castList(memoryCache.deleteByPrefix(primaryIndex, primaryKeys));
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
                redisModelCache.deleteByPrefix(primaryIndex, primaryKeys);
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
            long primaryRouteId = entity.getPrimaryRouteId();
            // 异步刷新
            scheduleFlush(cacheKey, primaryRouteId, PersistOp.DELETE);
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
                        doFlushDelete(entry);
                    } else {
                        Entity entity = entry.getSnapshot() != null ? castEntity(entry.getSnapshot()) : entityByCacheKey.get(entry.getCacheKey());
                        if (entity == null) {
                            continue;
                        }
                        doFlushUpsertEntity(entity);
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
    private void scheduleFlush(String cacheKey, long primaryRouteId, PersistOp op) {
        if (!meta.hasRedis() && !meta.hasDb()) {
            return;
        }
        dirtyTracker.mark(meta.getTableName(), cacheKey, primaryRouteId, op);
        doSubmitScheduleTask(cacheKey, primaryRouteId);
    }

    /** 提交任务 */
    private void doSubmitScheduleTask(String cacheKey, long primaryRouteId) {
        persistThreadPool.submit(primaryRouteId, meta.getTableName(), cacheKey, () -> {
            if (!dirtyTracker.isDirty(meta.getTableName(), cacheKey)) {
                return;
            }
            DirtyEntry entry = dirtyTracker.get(meta.getTableName(), cacheKey);
            if (entry == null) {
                return;
            }
            doFlush(entry);
            if (dirtyTracker.isDirty(meta.getTableName(), cacheKey)) {
                doSubmitScheduleTask(cacheKey, primaryRouteId);
            }
        });
    }

    @Override
    public void flushRoute(long routeId) {
        for (DirtyEntry entry : dirtyTracker.listByTableAndRouteId(meta.getTableName(), routeId)) {
            doFlush(entry);
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
            doFlush(entry);
        }
    }

    /**
     * 预加载分片数据（与 select 懒加载共用 {@link #ensureRouteLoaded}）
     * <p>仅 DB 策略无 JVM，不预加载</p>
     */
    @Override
    public void preload(long primaryRouteId) {
        if (!meta.isPreLoad() || !meta.hasJVM()) {
            return;
        }
        ensureRouteLoaded(primaryRouteId);
        LogTopic.MODEL.info("preload", "table", meta.getTableName(), "primaryRouteId", primaryRouteId,
            "loaded", memoryCache.isRouteLoaded(primaryRouteId),
            "hasData", memoryCache.hasRouteData(primaryRouteId));
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
        doFlush(entry);
    }

    private void syncFlush(String cacheKey, long primaryRouteId, PersistOp op) {
        DirtyEntry entry = dirtyTracker.get(meta.getTableName(), cacheKey);
        if (entry == null) {
            // persistNow 且尚未 mark dirty：构造临时条目，从内存取实体
            entry = new DirtyEntry(meta.getTableName(), cacheKey, primaryRouteId, op);
        }
        doFlush(entry);
    }

    /**
     * 执行单条 dirty flush；成功才 remove，失败保留以便重试
     */
    private void doFlush(DirtyEntry entry) {
        String cacheKey = entry.getCacheKey();
        PersistOp op = entry.getOp();
        try {
            boolean ok;
            if (op == PersistOp.DELETE) {
                doFlushDelete(entry);
                ok = true;
            } else {
                ok = doFlushUpsert(entry);
            }
            if (ok) {
                dirtyTracker.remove(meta.getTableName(), cacheKey);
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "flushFail", "table", meta.getTableName(), "cacheKey", cacheKey, "op", op);
        }
    }

    /** 执行清除 */
    private void doFlushDelete(DirtyEntry entry) {
        IndexMeta index = meta.getPrimaryIndex();
        Object[] keys = entry.getDeleteKeys() != null ? entry.getDeleteKeys() : parseKeysFromCacheKey(entry.getCacheKey(), index);
        long routeId = entry.getPrimaryRouteId();
        if (meta.hasRedis()) {
            redisModelCache.delete(routeId, index, keys);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).delete(meta, index, keys);
        }
    }

    /** 解析缓存cacheKey */
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

    /**
     * @return true 表示已写出或无需写出；false 表示找不到实体，保留 dirty
     */
    private boolean doFlushUpsert(DirtyEntry entry) {
        Entity entity = null;
        if (entry.getSnapshot() != null) {
            entity = castEntity(entry.getSnapshot());
        }
        if (entity == null) {
            entity = findEntityByCacheKey(entry.getCacheKey(), entry.getPrimaryRouteId());
        }
        if (entity == null) {
            LogTopic.MODEL.error("flushUpsertNoEntity", "table", meta.getTableName(),
                "cacheKey", entry.getCacheKey(), "routeId", entry.getPrimaryRouteId(),
                "hasSnapshot", entry.getSnapshot() != null);
            return false;
        }
        doFlushUpsertEntity(entity);
        return true;
    }

    /** 执行刷新实体 */
    private void doFlushUpsertEntity(Entity entity) {
        if (meta.hasRedis()) {
            redisModelCache.save(meta, entity);
        }
        if (meta.hasDb()) {
            persistEngineFactory.getEngine(meta).upsert(meta, entity);
        }
    }

    /** 从缓存中查找实体 */
    private Entity findEntityByCacheKey(String cacheKey, long routeId) {
        List<Entity> bucket = castList(memoryCache.getList(routeId, meta.getPrimaryIndex(), routeId));
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
