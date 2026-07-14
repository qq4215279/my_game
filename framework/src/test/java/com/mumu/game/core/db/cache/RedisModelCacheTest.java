package com.mumu.game.core.db.cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.db.DbTestSupport;
import com.mumu.game.core.db.bootstrap.ModelRegistry;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.cache.RedisModelCache;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.util.EntitySerializer;
import com.mumu.game.core.redis.RedisUtil;

/**
 * RedisModelCache CRUD 单测（mock RedisUtil）
 */
public class RedisModelCacheTest {

    private ModelMeta meta;
    private RedisModelCache cache;

    @BeforeMethod
    public void setUp() {
        meta = DbTestSupport.playerMeta();
        ModelRegistry.registerEntity(meta);
        cache = new RedisModelCache();
    }

    @Test
    public void save_getOne_delete_roundTrip() {
        Player player = DbTestSupport.newPlayer(1001L, "redisPlayer", 7);
        String redisKey = meta.buildRedisKey(1001L);
        String hashField = meta.buildHashField(player, meta.getPrimaryIndex());
        String json = EntitySerializer.serialize(meta, player);

        try (MockedStatic<RedisUtil> redis = mockStatic(RedisUtil.class)) {
            redis.when(() -> RedisUtil.hset(eq(redisKey), eq(hashField), eq(json), anyLong())).thenReturn(true);
            cache.save(meta, player);
            redis.verify(() -> RedisUtil.hset(eq(redisKey), eq(hashField), eq(json), anyLong()), times(1));

            redis.when(() -> RedisUtil.hGet(eq(redisKey), eq(hashField), any())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                java.util.function.Function<String, String> func = inv.getArgument(2);
                return func.apply(json);
            });

            BaseEntity loaded = cache.getOne(1001L, meta.getPrimaryIndex(), 1001L);
            Assert.assertNotNull(loaded);
            Assert.assertEquals(((Player) loaded).getName(), "redisPlayer");

            redis.when(() -> RedisUtil.hdel(eq(redisKey), eq(hashField))).thenAnswer(inv -> null);
            cache.delete(1001L, meta.getPrimaryIndex(), 1001L);
            redis.verify(() -> RedisUtil.hdel(eq(redisKey), eq(hashField)), times(1));
        }
    }

    @Test
    public void saveBatch_and_getList() {
        Player p1 = DbTestSupport.newPlayer(2001L, "a", 1);
        // 单字段主键 Redis Key 固定为 model:{table}；按 routeId 分桶，不同 playerId 会 hmset 两次同一 key
        Player p2 = DbTestSupport.newPlayer(2002L, "b", 2);

        String redisKey = meta.buildRedisKey(2001L);
        Assert.assertEquals(redisKey, meta.buildRedisKey(2002L));

        try (MockedStatic<RedisUtil> redis = mockStatic(RedisUtil.class)) {
            redis.when(() -> RedisUtil.hmset(anyString(), any(Map.class))).thenReturn(true);
            redis.when(() -> RedisUtil.expire(anyString(), anyLong())).thenReturn(true);
            cache.saveBatch(meta, List.of(p1, p2));
            redis.verify(() -> RedisUtil.hmset(eq(redisKey), any(Map.class)), times(2));

            Map<String, String> bucket = new HashMap<>();
            bucket.put(meta.buildHashField(p1, meta.getPrimaryIndex()), EntitySerializer.serialize(meta, p1));
            redis.when(() -> RedisUtil.hmget(eq(redisKey))).thenReturn(bucket);

            List<BaseEntity> list = cache.getList(2001L, meta.getPrimaryIndex(), 2001L);
            Assert.assertEquals(list.size(), 1);
            Assert.assertEquals(((Player) list.getFirst()).getName(), "a");
        }
    }

    @Test
    public void loadRouteBucket() {
        Player player = DbTestSupport.newPlayer(3001L, "load", 1);
        String redisKey = meta.buildRedisKey(3001L);
        String hashField = meta.buildHashField(player, meta.getPrimaryIndex());
        String json = EntitySerializer.serialize(meta, player);

        try (MockedStatic<RedisUtil> redis = mockStatic(RedisUtil.class)) {
            // 单字段主键走 hGet，避免整表 Hash
            redis.when(() -> RedisUtil.hGet(eq(redisKey), eq(hashField), any())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                java.util.function.Function<String, String> func = inv.getArgument(2);
                return func.apply(json);
            });
            Map<String, BaseEntity> loaded = cache.loadRouteBucket(meta, Player.class, 3001L);
            Assert.assertEquals(loaded.size(), 1);
            Assert.assertEquals(((Player) loaded.values().iterator().next()).getName(), "load");
        }
    }
}
