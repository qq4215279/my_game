package com.mumu.game.core.db.cache;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.cache.MemoryModelCache;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.example.PlayerTemplate;

/**
 * MemoryModelCache 单测
 */
public class MemoryModelCacheTest {

    private ModelMeta playerMeta;
    private ModelMeta templateMeta;
    private MemoryModelCache<Player> playerCache;
    private MemoryModelCache<PlayerTemplate> templateCache;

    @BeforeMethod
    public void setUp() {
        playerMeta = DbTestSupport.playerMeta();
        templateMeta = DbTestSupport.playerTemplateMeta();
        playerCache = new MemoryModelCache<>(playerMeta);
        templateCache = new MemoryModelCache<>(templateMeta);
    }

    @Test
    public void save_getOne_delete() {
        Player player = DbTestSupport.newPlayer(11L, "hero", 5);
        playerCache.save(playerMeta, player);

        Player loaded = playerCache.getOne(11L, playerMeta.getPrimaryIndex(), 11L);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(loaded.getName(), "hero");
        Assert.assertSame(loaded, player);

        playerCache.delete(11L, playerMeta.getPrimaryIndex(), 11L);
        Assert.assertNull(playerCache.getOne(11L, playerMeta.getPrimaryIndex(), 11L));
        // 删空后仍保留「已加载」空桶
        Assert.assertTrue(playerCache.isRouteLoaded(11L));
        Assert.assertFalse(playerCache.hasRouteData(11L));
    }

    @Test
    public void markRouteLoaded_emptyBucket_meansLoaded() {
        Assert.assertFalse(playerCache.isRouteLoaded(99L));
        playerCache.markRouteLoaded(99L);
        Assert.assertTrue(playerCache.isRouteLoaded(99L));
        Assert.assertFalse(playerCache.hasRouteData(99L));
        Assert.assertNull(playerCache.getOne(99L, playerMeta.getPrimaryIndex(), 99L));

        playerCache.clearRoute(99L);
        Assert.assertFalse(playerCache.isRouteLoaded(99L));
    }

    @Test
    public void secondaryIndex_query_and_deleteByPrefix() {
        PlayerTemplate t1 = DbTestSupport.newPlayerTemplate(20L, 1, 100);
        PlayerTemplate t2 = DbTestSupport.newPlayerTemplate(20L, 2, 200);
        templateCache.save(templateMeta, t1);
        templateCache.save(templateMeta, t2);

        IndexMeta secondary = templateMeta.getIndex("playerid_activityid");
        PlayerTemplate hit = templateCache.getOne(20L, secondary, 20L, 100);
        Assert.assertNotNull(hit);
        Assert.assertEquals(hit.getFunctionId(), 1);

        List<BaseEntity> list = templateCache.getList(20L, templateMeta.getPrimaryIndex(), 20L);
        Assert.assertEquals(list.size(), 2);

        List<BaseEntity> removed = templateCache.deleteByPrefix(templateMeta.getPrimaryIndex(), 20L);
        Assert.assertEquals(removed.size(), 2);
        Assert.assertTrue(templateCache.getList(20L, templateMeta.getPrimaryIndex(), 20L).isEmpty());
    }

    @Test
    public void saveBatch_and_clearRoute() {
        Player p1 = DbTestSupport.newPlayer(31L, "a", 1);
        Player p2 = DbTestSupport.newPlayer(31L, "b", 2);
        // 同 route 不同主键：player 是单字段主键，route=playerId，两条不同 id
        p2.setPlayerId(32L);
        playerCache.saveBatch(playerMeta, List.of(p1, p2));
        Assert.assertNotNull(playerCache.getOne(31L, playerMeta.getPrimaryIndex(), 31L));
        Assert.assertNotNull(playerCache.getOne(32L, playerMeta.getPrimaryIndex(), 32L));

        playerCache.clearRoute(31L);
        Assert.assertNull(playerCache.getOne(31L, playerMeta.getPrimaryIndex(), 31L));
        Assert.assertNotNull(playerCache.getOne(32L, playerMeta.getPrimaryIndex(), 32L));
    }
}
