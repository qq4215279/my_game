package com.mumu.game.core.db.index;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.core.index.MultiSecondaryIndex;
import com.mumu.game.core.db.core.index.PrimaryIndex;
import com.mumu.game.core.db.core.index.UniqueSecondaryIndex;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.example.PlayerTemplate;

/**
 * 桶内索引结构单测
 */
public class ModelIndexTest {

    private ModelMeta playerMeta;
    private ModelMeta templateMeta;

    @BeforeMethod
    public void setUp() {
        playerMeta = DbTestSupport.playerMeta();
        templateMeta = DbTestSupport.playerTemplateMeta();
    }

    @Test
    public void primaryIndex_putGetRemove_andLeftFind() {
        AtomicReference<Player> evicted = new AtomicReference<>();
        PrimaryIndex<Player> primary = new PrimaryIndex<>(Integer.MAX_VALUE, evicted::set);

        Player p1 = DbTestSupport.newPlayer(1L, "a", 1);
        Player p2 = DbTestSupport.newPlayer(2L, "b", 2);
        String f1 = playerMeta.buildHashField(p1, playerMeta.getPrimaryIndex());
        String f2 = playerMeta.buildHashField(p2, playerMeta.getPrimaryIndex());

        Assert.assertNull(primary.put(f1, p1));
        primary.put(f2, p2);
        Assert.assertEquals(primary.getOne(f1).getName(), "a");
        Assert.assertEquals(primary.size(), 2);

        IndexMeta index = playerMeta.getPrimaryIndex();
        List<Player> list = primary.leftFind(index, 1L);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.getFirst().getPlayerId(), 1L);

        Assert.assertEquals(primary.removeKey(f1), p1);
        Assert.assertNull(primary.getOne(f1));
        Assert.assertNull(evicted.get());
    }

    @Test
    public void uniqueSecondaryIndex_crud_andLeftFind() {
        UniqueSecondaryIndex<PlayerTemplate> index = new UniqueSecondaryIndex<>();
        IndexMeta secondary = templateMeta.getIndex("playerid_activityid");
        PlayerTemplate e1 = DbTestSupport.newPlayerTemplate(1L, 10, 100);
        PlayerTemplate e2 = DbTestSupport.newPlayerTemplate(1L, 11, 101);

        String k1 = secondary.buildIndexKey(e1);
        String k2 = secondary.buildIndexKey(e2);
        index.put(k1, e1);
        index.put(k2, e2);

        Assert.assertEquals(index.getOne(k1).getFunctionId(), 10);
        Assert.assertEquals(index.getAll(k1).size(), 1);

        List<PlayerTemplate> prefix = index.leftFind(secondary, 1L);
        Assert.assertEquals(prefix.size(), 2);

        index.remove(k1, e1);
        Assert.assertNull(index.getOne(k1));
    }

    @Test
    public void multiSecondaryIndex_allowsSameKey() {
        MultiSecondaryIndex<PlayerTemplate> index = new MultiSecondaryIndex<>();
        PlayerTemplate e1 = DbTestSupport.newPlayerTemplate(1L, 1, 1);
        PlayerTemplate e2 = DbTestSupport.newPlayerTemplate(1L, 2, 1);
        index.put("1:1:", e1);
        index.put("1:1:", e2);

        Assert.assertEquals(index.getAll("1:1:").size(), 2);
        index.remove("1:1:", e1);
        Assert.assertEquals(index.getAll("1:1:").size(), 1);
        Assert.assertEquals(index.getOne("1:1:").getFunctionId(), 2);
    }
}
