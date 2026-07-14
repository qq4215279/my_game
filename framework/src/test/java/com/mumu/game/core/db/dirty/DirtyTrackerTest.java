package com.mumu.game.core.db.dirty;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.consts.PersistOp;
import com.mumu.game.core.db.core.dirty.DirtyEntry;
import com.mumu.game.core.db.core.dirty.DirtyTracker;

/**
 * DirtyTracker 单测
 */
public class DirtyTrackerTest {

    private DirtyTracker tracker;

    @BeforeMethod
    public void setUp() {
        tracker = new DirtyTracker();
    }

    @Test
    public void mark_merge_and_remove() {
        tracker.mark("player", "player:pk:1", 1L, PersistOp.INSERT);
        tracker.mark("player", "player:pk:1", 1L, PersistOp.UPDATE);

        Assert.assertTrue(tracker.isDirty("player", "player:pk:1"));
        DirtyEntry entry = tracker.get("player", "player:pk:1");
        Assert.assertNotNull(entry);
        Assert.assertEquals(entry.getPrimaryRouteId(), 1L);

        tracker.remove("player", "player:pk:1");
        Assert.assertFalse(tracker.isDirty("player", "player:pk:1"));
    }

    @Test
    public void listByTableAndRouteId_andPortion() {
        tracker.mark("player", "k1", 1L, PersistOp.UPDATE);
        tracker.mark("player", "k2", 2L, PersistOp.UPDATE);
        tracker.mark("player", "k3", 3L, PersistOp.DELETE);

        List<DirtyEntry> route1 = tracker.listByTableAndRouteId("player", 1L);
        Assert.assertEquals(route1.size(), 1);
        Assert.assertEquals(route1.getFirst().getCacheKey(), "k1");

        List<DirtyEntry> portion = tracker.listByTableAndPortion("player", 1, 2);
        // routeId % 2 == 1 → k1(1), k3(3)
        Assert.assertEquals(portion.size(), 2);
    }

    @Test
    public void mark_withSnapshot_mergeUpdatesSnapshot() {
        com.mumu.game.core.db.DbOnlyPlayerEntity snap1 = new com.mumu.game.core.db.DbOnlyPlayerEntity();
        snap1.setPlayerId(1L);
        snap1.setName("a");
        com.mumu.game.core.db.DbOnlyPlayerEntity snap2 = new com.mumu.game.core.db.DbOnlyPlayerEntity();
        snap2.setPlayerId(1L);
        snap2.setName("b");

        tracker.mark("db_only_player", "k", 1L, PersistOp.INSERT, snap1, null);
        tracker.mark("db_only_player", "k", 1L, PersistOp.UPDATE, snap2, null);

        DirtyEntry entry = tracker.get("db_only_player", "k");
        Assert.assertNotNull(entry);
        Assert.assertEquals(entry.getOp(), PersistOp.INSERT);
        Assert.assertSame(entry.getSnapshot(), snap2);

        tracker.mark("db_only_player", "k", 1L, PersistOp.DELETE, null, new Object[]{1L});
        Assert.assertEquals(entry.getOp(), PersistOp.DELETE);
        Assert.assertNull(entry.getSnapshot());
        Assert.assertEquals(entry.getDeleteKeys()[0], 1L);
    }
}
