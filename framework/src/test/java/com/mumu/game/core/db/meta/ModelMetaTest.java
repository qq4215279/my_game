package com.mumu.game.core.db.meta;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.example.PlayerTemplate;

/**
 * ModelMeta / IndexMeta 单测
 */
public class ModelMetaTest {

    private ModelMeta playerMeta;
    private ModelMeta templateMeta;

    @BeforeMethod
    public void setUp() {
        playerMeta = DbTestSupport.playerMeta();
        templateMeta = DbTestSupport.playerTemplateMeta();
    }

    @Test
    public void player_singleFieldPrimary_routeAndKeys() {
        Assert.assertEquals(playerMeta.getTableName(), "player");
        Assert.assertTrue(playerMeta.isSingleFieldPrimary());
        Assert.assertTrue(playerMeta.hasRedis());
        Assert.assertTrue(playerMeta.hasDb());
        Assert.assertTrue(playerMeta.hasJVM());

        Player player = DbTestSupport.newPlayer(1001L, "tom", 10);
        Assert.assertEquals(player.getPrimaryRouteId(), 1001L);
        Assert.assertEquals(playerMeta.getRouteId(1001L), 1001L);

        IndexMeta primary = playerMeta.getPrimaryIndex();
        Assert.assertTrue(primary.isPrimary());
        Assert.assertTrue(primary.isFullKey(1001L));
        Assert.assertFalse(primary.isFullKey());
        Object[] keys = primary.readKeyValues(player);
        Assert.assertEquals(keys.length, 1);
        Assert.assertEquals(((Number) keys[0]).longValue(), 1001L);
    }

    @Test
    public void playerTemplate_compositePrimary_andSecondary() {
        Assert.assertFalse(templateMeta.isSingleFieldPrimary());
        Assert.assertEquals(templateMeta.getSecondaryIndexes().size(), 1);

        PlayerTemplate entity = DbTestSupport.newPlayerTemplate(7L, 101, 202);
        Assert.assertEquals(entity.getPrimaryRouteId(), 7L);
        Assert.assertEquals(templateMeta.getRouteId(7L, 101), 7L);

        IndexMeta primary = templateMeta.getPrimaryIndex();
        Assert.assertTrue(primary.isFullKey(7L, 101));
        Assert.assertTrue(primary.matchPrefix(entity, 7L));
        Assert.assertTrue(primary.matchPrefix(entity, 7L, 101));
        Assert.assertFalse(primary.matchPrefix(entity, 8L));

        IndexMeta secondary = templateMeta.getIndex("playerid_activityid");
        Assert.assertFalse(secondary.isPrimary());
        Assert.assertTrue(secondary.matchPrefix(entity, 7L, 202));
        String indexKey = secondary.buildIndexKey(7L, 202);
        Assert.assertTrue(indexKey.startsWith("7:202"));
    }

    @Test
    public void buildCacheKey_andHashField() {
        Player player = DbTestSupport.newPlayer(9L, "a", 1);
        String cacheKey = playerMeta.buildCacheKey(player);
        Assert.assertTrue(cacheKey.contains("player"));
        Assert.assertTrue(cacheKey.contains("9"));

        String hashField = playerMeta.buildHashField(player, playerMeta.getPrimaryIndex());
        Assert.assertNotNull(hashField);
    }
}
