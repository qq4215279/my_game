package com.mumu.game.core.db.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.DbOnlyPlayerEntity;
import com.mumu.game.core.db.DbOnlyPlayerModel;
import com.mumu.game.core.db.core.cache.RedisModelCache;
import com.mumu.game.core.db.core.dirty.DirtyTracker;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.core.persist.PersistEngine;
import com.mumu.game.core.db.core.persist.PersistEngineFactory;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.example.PlayerModel;
import com.mumu.game.core.db.lifecycle.PersistThreadPool;
import com.mumu.game.core.db.util.ModelRouteChecker;

/**
 * BaseModel 单测（mock Redis / Persist / 线程池）
 */
public class BaseModelTest {

    private PlayerModel model;
    private ModelMeta meta;
    private RedisModelCache redisModelCache;
    private PersistEngine persistEngine;
    private DirtyTracker dirtyTracker;

    @BeforeMethod
    public void setUp() {
        meta = DbTestSupport.playerMeta();
        model = new PlayerModel();
        model.bindMeta(meta);

        redisModelCache = mock(RedisModelCache.class);
        persistEngine = mock(PersistEngine.class);
        PersistEngineFactory engineFactory = mock(PersistEngineFactory.class);
        when(engineFactory.getEngine(meta)).thenReturn(persistEngine);

        dirtyTracker = new DirtyTracker();
        PersistThreadPool persistThreadPool = mock(PersistThreadPool.class);
        doNothing().when(persistThreadPool).submit(anyLong(), anyString(), anyString(), any(Runnable.class));

        ModelRouteChecker routeChecker = mock(ModelRouteChecker.class);
        when(routeChecker.checkWrite(anyLong(), eq(meta))).thenReturn(true);

        DbTestSupport.setField(model, "redisModelCache", redisModelCache);
        DbTestSupport.setField(model, "persistEngineFactory", engineFactory);
        DbTestSupport.setField(model, "dirtyTracker", dirtyTracker);
        DbTestSupport.setField(model, "persistThreadPool", persistThreadPool);
        DbTestSupport.setField(model, "modelRouteChecker", routeChecker);
    }

    @Test
    public void insert_then_selectOne_fromMemory() {
        Player player = DbTestSupport.newPlayer(101L, "p", 1);
        model.insert(player, false);

        Player loaded = model.selectOne(101L);
        Assert.assertNotNull(loaded);
        Assert.assertSame(loaded, player);
        Assert.assertTrue(dirtyTracker.isDirty(meta.getTableName(), meta.buildCacheKey(player)));
        // insert 已建桶，不应再穿透
        verify(redisModelCache, times(0)).loadRouteBucket(any(), any(), anyLong());
    }

    @Test
    public void selectOne_loadFromRedis_andBackfillMemory() {
        Player redisPlayer = DbTestSupport.newPlayer(202L, "fromRedis", 3);
        String field = meta.buildHashField(redisPlayer, meta.getPrimaryIndex());
        when(redisModelCache.loadRouteBucket(eq(meta), eq(Player.class), eq(202L)))
            .thenReturn(Map.of(field, redisPlayer));

        Player loaded = model.selectOne(202L);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(loaded.getName(), "fromRedis");
        // 再次 select 应命中内存，不再 loadRouteBucket
        Assert.assertSame(model.selectOne(202L), loaded);
        verify(redisModelCache, times(1)).loadRouteBucket(eq(meta), eq(Player.class), eq(202L));
        verify(persistEngine, times(0)).findList(any(), any(), any());
    }

    @Test
    public void selectOne_loadFromDb_whenRedisMiss() {
        when(redisModelCache.loadRouteBucket(eq(meta), eq(Player.class), eq(303L)))
            .thenReturn(Collections.emptyMap());
        Player dbPlayer = DbTestSupport.newPlayer(303L, "fromDb", 9);
        when(persistEngine.findList(eq(meta), eq(meta.getPrimaryIndex()), eq(303L)))
            .thenReturn(List.of(dbPlayer));

        Player loaded = model.selectOne(303L);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(loaded.getName(), "fromDb");
        verify(persistEngine).findList(eq(meta), eq(meta.getPrimaryIndex()), eq(303L));
        // 回填 Redis
        verify(redisModelCache).saveBatch(eq(meta), any());
    }

    @Test
    public void selectOne_emptyLoaded_doesNotHitStoreAgain() {
        when(redisModelCache.loadRouteBucket(eq(meta), eq(Player.class), eq(606L)))
            .thenReturn(Collections.emptyMap());
        when(persistEngine.findList(eq(meta), eq(meta.getPrimaryIndex()), eq(606L)))
            .thenReturn(Collections.emptyList());

        Assert.assertNull(model.selectOne(606L));
        Assert.assertNull(model.selectOne(606L));
        Assert.assertTrue(model.selectList(606L).isEmpty());

        verify(redisModelCache, times(1)).loadRouteBucket(eq(meta), eq(Player.class), eq(606L));
        verify(persistEngine, times(1)).findList(eq(meta), eq(meta.getPrimaryIndex()), eq(606L));
    }

    @Test
    public void update_and_delete_softPath() {
        Player player = DbTestSupport.newPlayer(404L, "u", 1);
        model.insert(player, false);
        player.setLevel(99);
        model.update(player, false);
        Assert.assertEquals(model.selectOne(404L).getLevel(), 99);

        model.delete(player, false);
        // 删空后保留已加载标记，select 为 null 且不再穿透
        Assert.assertNull(model.selectOne(404L));
        verify(redisModelCache, times(0)).loadRouteBucket(any(), any(), eq(404L));
    }

    @Test
    public void insertOrUpdate_whenExistsRefMismatch_keepsOld() {
        Player cached = DbTestSupport.newPlayer(505L, "old", 1);
        model.insert(cached, false);

        Player other = DbTestSupport.newPlayer(505L, "new", 2);
        model.insertOrUpdate(other);
        Assert.assertSame(model.selectOne(505L), cached);
        Assert.assertEquals(model.selectOne(505L).getName(), "old");
    }

    @Test
    public void dbOnly_selectAlwaysHitsEngine_noCache() {
        ModelMeta dbMeta = DbTestSupport.dbOnlyPlayerMeta();
        Assert.assertFalse(dbMeta.hasJVM());
        Assert.assertFalse(dbMeta.hasRedis());
        Assert.assertTrue(dbMeta.hasDb());

        DbOnlyPlayerModel dbModel = new DbOnlyPlayerModel();
        dbModel.bindMeta(dbMeta);

        PersistEngine dbEngine = mock(PersistEngine.class);
        PersistEngineFactory dbFactory = mock(PersistEngineFactory.class);
        when(dbFactory.getEngine(dbMeta)).thenReturn(dbEngine);
        RedisModelCache unusedRedis = mock(RedisModelCache.class);
        ModelRouteChecker routeChecker = mock(ModelRouteChecker.class);
        when(routeChecker.checkWrite(anyLong(), eq(dbMeta))).thenReturn(true);

        DbTestSupport.setField(dbModel, "redisModelCache", unusedRedis);
        DbTestSupport.setField(dbModel, "persistEngineFactory", dbFactory);
        DbTestSupport.setField(dbModel, "dirtyTracker", new DirtyTracker());
        DbTestSupport.setField(dbModel, "persistThreadPool", mock(PersistThreadPool.class));
        DbTestSupport.setField(dbModel, "modelRouteChecker", routeChecker);

        DbOnlyPlayerEntity row = new DbOnlyPlayerEntity();
        row.setPlayerId(707L);
        row.setName("dbOnly");
        when(dbEngine.findOne(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L))).thenReturn(row);
        when(dbEngine.findList(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L))).thenReturn(List.of(row));

        Assert.assertEquals(dbModel.selectOne(707L).getName(), "dbOnly");
        Assert.assertEquals(dbModel.selectOne(707L).getName(), "dbOnly");
        Assert.assertEquals(dbModel.selectList(707L).size(), 1);

        // 每次 select 都打引擎，无「已加载」短路；不走 Redis 灌桶
        verify(dbEngine, times(2)).findOne(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L));
        verify(dbEngine, times(1)).findList(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L));
        verify(unusedRedis, times(0)).loadRouteBucket(any(), any(), anyLong());

        // 误配 preLoad 也不灌缓存 / 不再打引擎
        dbModel.preload(707L);
        verify(unusedRedis, times(0)).loadRouteBucket(any(), any(), anyLong());
        verify(dbEngine, times(1)).findList(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L));
        verify(dbEngine, times(2)).findOne(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(707L));
    }

    @Test
    public void dbOnly_insertUpdateDelete_asyncViaDirtySnapshot() {
        ModelMeta dbMeta = DbTestSupport.dbOnlyPlayerMeta();
        DbOnlyPlayerModel dbModel = new DbOnlyPlayerModel();
        dbModel.bindMeta(dbMeta);

        PersistEngine dbEngine = mock(PersistEngine.class);
        PersistEngineFactory dbFactory = mock(PersistEngineFactory.class);
        when(dbFactory.getEngine(dbMeta)).thenReturn(dbEngine);
        ModelRouteChecker routeChecker = mock(ModelRouteChecker.class);
        when(routeChecker.checkWrite(anyLong(), eq(dbMeta))).thenReturn(true);

        DirtyTracker dbDirty = new DirtyTracker();
        PersistThreadPool pool = mock(PersistThreadPool.class);
        // 提交即执行，模拟异步落到业务线程
        org.mockito.Mockito.doAnswer(inv -> {
            Runnable task = inv.getArgument(3);
            task.run();
            return null;
        }).when(pool).submit(anyLong(), anyString(), anyString(), any(Runnable.class));

        DbTestSupport.setField(dbModel, "redisModelCache", mock(RedisModelCache.class));
        DbTestSupport.setField(dbModel, "persistEngineFactory", dbFactory);
        DbTestSupport.setField(dbModel, "dirtyTracker", dbDirty);
        DbTestSupport.setField(dbModel, "persistThreadPool", pool);
        DbTestSupport.setField(dbModel, "modelRouteChecker", routeChecker);

        DbOnlyPlayerEntity row = new DbOnlyPlayerEntity();
        row.setPlayerId(808L);
        row.setName("w");
        dbModel.insert(row, false);
        verify(dbEngine).upsert(eq(dbMeta), any(DbOnlyPlayerEntity.class));
        Assert.assertFalse(dbDirty.isDirty(dbMeta.getTableName(), dbMeta.buildCacheKey(row)));

        row.setName("w2");
        dbModel.update(row, false);
        verify(dbEngine, times(2)).upsert(eq(dbMeta), any(DbOnlyPlayerEntity.class));

        dbModel.delete(row, false);
        verify(dbEngine).delete(eq(dbMeta), eq(dbMeta.getPrimaryIndex()), eq(808L));
    }

    @Test
    public void dbOnly_persistNow_syncEngine() {
        ModelMeta dbMeta = DbTestSupport.dbOnlyPlayerMeta();
        DbOnlyPlayerModel dbModel = new DbOnlyPlayerModel();
        dbModel.bindMeta(dbMeta);

        PersistEngine dbEngine = mock(PersistEngine.class);
        PersistEngineFactory dbFactory = mock(PersistEngineFactory.class);
        when(dbFactory.getEngine(dbMeta)).thenReturn(dbEngine);
        ModelRouteChecker routeChecker = mock(ModelRouteChecker.class);
        when(routeChecker.checkWrite(anyLong(), eq(dbMeta))).thenReturn(true);
        PersistThreadPool pool = mock(PersistThreadPool.class);

        DbTestSupport.setField(dbModel, "redisModelCache", mock(RedisModelCache.class));
        DbTestSupport.setField(dbModel, "persistEngineFactory", dbFactory);
        DbTestSupport.setField(dbModel, "dirtyTracker", new DirtyTracker());
        DbTestSupport.setField(dbModel, "persistThreadPool", pool);
        DbTestSupport.setField(dbModel, "modelRouteChecker", routeChecker);

        DbOnlyPlayerEntity row = new DbOnlyPlayerEntity();
        row.setPlayerId(909L);
        row.setName("sync");
        dbModel.insert(row, true);
        verify(dbEngine).upsert(eq(dbMeta), eq(row));
        verify(pool, times(0)).submit(anyLong(), anyString(), anyString(), any(Runnable.class));
    }
}
