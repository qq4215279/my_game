package com.mumu.game.core.db.core.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.core.BaseEntity;

/**
 * UniqueSecondaryIndex
 * 唯一副索引：indexKey → entity
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public final class UniqueSecondaryIndex<Entity extends BaseEntity> implements SecondaryIndex<Entity> {

    private final Map<String, Entity> map = new HashMap<>();

    @Override
    public Entity put(String indexKey, Entity entity) {
        return map.put(indexKey, entity);
    }

    @Override
    public void remove(String indexKey, Entity entity) {
        Entity current = map.get(indexKey);
        if (current == entity) {
            map.remove(indexKey);
        }
    }

    @Override
    public Entity getOne(String indexKey) {
        return map.get(indexKey);
    }

    @Override
    public List<Entity> getAll(String indexKey) {
        Entity entity = map.get(indexKey);
        return entity == null ? Collections.emptyList() : List.of(entity);
    }

    @Override
    public List<Entity> leftFind(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entity> result = new ArrayList<>();
        for (Map.Entry<String, Entity> entry : map.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
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
        return map.size();
    }

    @Override
    public Collection<Entity> values() {
        return map.values();
    }
}
