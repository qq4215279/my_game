package com.mumu.game.core.db.core.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mumu.game.collection.LRULinkedHashMap;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;

/**
 * PrimaryIndex
 * 路由桶主存储（hashField → entity），支持 capacity LRU 淘汰
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public final class PrimaryIndex<Entity extends BaseEntity> implements ModelIndex<Entity> {

    private final Map<String, Entity> store;

    /**
     * @param capacity 桶内容量；{@code >= Integer.MAX_VALUE} 表示不限制（普通 HashMap）
     * @param onEvict  LRU 淘汰回调（卸副索引 + 业务 DELETE）
     */
    public PrimaryIndex(int capacity, Consumer<Entity> onEvict) {
        if (capacity == Integer.MAX_VALUE) {
            this.store = new HashMap<>();
        } else {
            this.store = LRULinkedHashMap.of(capacity, (field, entity) -> {
                if (onEvict != null) {
                    onEvict.accept(entity);
                }
            });
        }
    }

    @Override
    public Entity getOne(String indexKey) {
        return store.get(indexKey);
    }

    @Override
    public List<Entity> getAll(String indexKey) {
        Entity entity = store.get(indexKey);
        return entity == null ? Collections.emptyList() : List.of(entity);
    }

    @Override
    public Entity put(String indexKey, Entity entity) {
        return store.put(indexKey, entity);
    }

    @Override
    public void remove(String indexKey, Entity entity) {
        Entity current = store.get(indexKey);
        if (current == entity) {
            store.remove(indexKey);
        }
    }

    /**
     * 按 hashField 删除（不校验实体引用）
     */
    public Entity removeKey(String hashField) {
        return store.remove(hashField);
    }

    @Override
    public List<Entity> leftFind(IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0) {
            return Collections.emptyList();
        }
        // 主索引左前缀：用字段 matchPrefix 扫实体，勿对 hashField 做 startsWith
        List<Entity> result = new ArrayList<>();
        for (Entity entity : store.values()) {
            if (index.matchPrefix(entity, keys)) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public Collection<Entity> values() {
        return store.values();
    }
}
