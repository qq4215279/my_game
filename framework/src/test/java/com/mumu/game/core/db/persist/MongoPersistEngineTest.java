package com.mumu.game.core.db.persist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.core.persist.engine.MongoPersistEngine;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.mongo.config.MongoDB;

/**
 * MongoPersistEngine CRUD 单测（mock MongoTemplate）
 */
public class MongoPersistEngineTest {

    private ModelMeta meta;
    private MongoPersistEngine engine;
    private MongoTemplate template;
    private Map<String, MongoTemplate> previousMap;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        meta = DbTestSupport.playerMeta();
        engine = new MongoPersistEngine();
        template = mock(MongoTemplate.class);

        Field field = MongoDB.class.getDeclaredField("mongoTemplateMap");
        field.setAccessible(true);
        previousMap = (Map<String, MongoTemplate>) field.get(null);
        Map<String, MongoTemplate> map = new HashMap<>();
        map.put(MongoDB.MODEL.getDatabase(), template);
        field.set(null, map);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Field field = MongoDB.class.getDeclaredField("mongoTemplateMap");
        field.setAccessible(true);
        field.set(null, previousMap);
    }

    @Test
    public void findOne_and_findList() {
        Player player = DbTestSupport.newPlayer(1L, "m", 1);
        when(template.findOne(any(Query.class), eq(Player.class), eq("player"))).thenReturn(player);
        when(template.find(any(Query.class), eq(Player.class), eq("player"))).thenReturn(List.of(player));

        Player one = engine.findOne(meta, meta.getPrimaryIndex(), 1L);
        Assert.assertNotNull(one);
        Assert.assertEquals(one.getName(), "m");

        List<Player> list = engine.findList(meta, meta.getPrimaryIndex(), 1L);
        Assert.assertEquals(list.size(), 1);
    }

    @Test
    public void upsert_usesFindAndReplace() {
        Player player = DbTestSupport.newPlayer(2L, "u", 2);
        when(template.findAndReplace(any(Query.class), eq(player), any(FindAndReplaceOptions.class),
            eq(Player.class), eq("player"))).thenReturn(player);

        engine.upsert(meta, player);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(template).findAndReplace(queryCaptor.capture(), eq(player), any(FindAndReplaceOptions.class),
            eq(Player.class), eq("player"));
        Assert.assertNotNull(queryCaptor.getValue());
    }

    @Test
    public void upsertBatch_usesBulkReplaceOne() {
        Player p1 = DbTestSupport.newPlayer(3L, "a", 1);
        Player p2 = DbTestSupport.newPlayer(4L, "b", 1);
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(template.bulkOps(any(), eq(Player.class), eq("player"))).thenReturn(bulkOps);
        when(bulkOps.replaceOne(any(Query.class), any(), any(FindAndReplaceOptions.class))).thenReturn(bulkOps);

        engine.upsertBatch(meta, List.of(p1, p2));

        verify(bulkOps).execute();
    }

    @Test
    public void delete_and_deleteByPrefix() {
        engine.delete(meta, meta.getPrimaryIndex(), 5L);
        verify(template).remove(any(Query.class), eq("player"));

        engine.deleteByPrefix(meta, meta.getPrimaryIndex(), 5L);
        verify(template, org.mockito.Mockito.times(2)).remove(any(Query.class), eq("player"));
    }
}
