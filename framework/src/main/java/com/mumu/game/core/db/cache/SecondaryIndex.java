package com.mumu.game.core.db.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mumu.game.core.db.core.BaseEntity;

/**
 * SecondaryIndex
 * 路由桶内非主索引（空间换时间，完整键 O(1)，左前缀扫索引 key）
 * <p>只存实体引用，不拷贝对象</p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/13
 */
final class SecondaryIndex<Entity extends BaseEntity> {

    private final boolean unique;
    /** 唯一索引：indexKey → entity */
    private final Map<String, Entity> uniqueMap;
    /** 非唯一索引：indexKey → entities */
    private final Map<String, List<Entity>> multiMap;

    SecondaryIndex(boolean unique) {
        this.unique = unique;
        if (unique) {
            this.uniqueMap = new HashMap<>();
            this.multiMap = null;
        } else {
            this.uniqueMap = null;
            this.multiMap = new HashMap<>();
        }
    }

    void put(String indexKey, Entity entity) {
        if (unique) {
            uniqueMap.put(indexKey, entity);
            return;
        }
        multiMap.computeIfAbsent(indexKey, k -> new ArrayList<>(2)).add(entity);
    }

    void remove(String indexKey, Entity entity) {
        if (unique) {
            Entity current = uniqueMap.get(indexKey);
            if (current == entity) {
                uniqueMap.remove(indexKey);
            }
            return;
        }
        List<Entity> list = multiMap.get(indexKey);
        if (list == null) {
            return;
        }
        list.remove(entity);
        if (list.isEmpty()) {
            multiMap.remove(indexKey);
        }
    }

    /**
     * 完整键查询：唯一索引返回单条；非唯一返回列表（可能多条）
     */
    Entity getOne(String indexKey) {
        if (unique) {
            return uniqueMap.get(indexKey);
        }
        List<Entity> list = multiMap.get(indexKey);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    List<Entity> getAll(String indexKey) {
        if (unique) {
            Entity entity = uniqueMap.get(indexKey);
            return entity == null ? Collections.emptyList() : List.of(entity);
        }
        List<Entity> list = multiMap.get(indexKey);
        return list == null || list.isEmpty() ? Collections.emptyList() : List.copyOf(list);
    }

    /**
     * 左前缀查询（扫索引 key，比扫全部实体更轻）
     */
    List<Entity> leftFind(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entity> result = new ArrayList<>();
        if (unique) {
            for (Map.Entry<String, Entity> entry : uniqueMap.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    result.add(entry.getValue());
                }
            }
        } else {
            for (Map.Entry<String, List<Entity>> entry : multiMap.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    result.addAll(entry.getValue());
                }
            }
        }
        return result;
    }
}
