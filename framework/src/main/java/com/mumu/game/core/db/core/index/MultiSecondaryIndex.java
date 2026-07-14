package com.mumu.game.core.db.core.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.core.BaseEntity;

/**
 * MultiSecondaryIndex
 * 非唯一副索引：indexKey → entities
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public final class MultiSecondaryIndex<Entity extends BaseEntity> implements SecondaryIndex<Entity> {

    private final Map<String, List<Entity>> map = new HashMap<>();

    @Override
    public Entity put(String indexKey, Entity entity) {
        map.computeIfAbsent(indexKey, k -> new ArrayList<>(2)).add(entity);
        return null;
    }

    @Override
    public void remove(String indexKey, Entity entity) {
        List<Entity> list = map.get(indexKey);
        if (list == null) {
            return;
        }
        list.remove(entity);
        if (list.isEmpty()) {
            map.remove(indexKey);
        }
    }

    @Override
    public Entity getOne(String indexKey) {
        List<Entity> list = map.get(indexKey);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public List<Entity> getAll(String indexKey) {
        List<Entity> list = map.get(indexKey);
        return list == null || list.isEmpty() ? Collections.emptyList() : List.copyOf(list);
    }

    @Override
    public List<Entity> leftFind(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entity> result = new ArrayList<>();
        for (Map.Entry<String, List<Entity>> entry : map.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        int total = 0;
        for (List<Entity> list : map.values()) {
            total += list.size();
        }
        return total;
    }

    @Override
    public Collection<Entity> values() {
        List<Entity> result = new ArrayList<>(size());
        for (List<Entity> list : map.values()) {
            result.addAll(list);
        }
        return result;
    }
}
