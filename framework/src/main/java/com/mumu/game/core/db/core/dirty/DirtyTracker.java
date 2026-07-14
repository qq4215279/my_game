package com.mumu.game.core.db.core.dirty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.mumu.game.core.db.consts.PersistOp;
import com.mumu.game.core.db.core.BaseEntity;

/**
 * DirtyTracker
 * 脏数据追踪（同一条记录仅保留一个标记）
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class DirtyTracker {
    /** tableName -> cacheKey -> entry */
    private final Map<String, Map<String, DirtyEntry>> dirtyMap = new ConcurrentHashMap<>();

    /**
     * 标记脏数据
     */
    public void mark(String tableName, String cacheKey, long primaryRouteId, PersistOp op) {
        mark(tableName, cacheKey, primaryRouteId, op, null, null);
    }

    /**
     * 标记脏数据；可附带实体快照（upsert）或删除键（仅 DB 异步写）
     */
    public void mark(String tableName, String cacheKey, long primaryRouteId, PersistOp op,
                     BaseEntity snapshot, Object[] deleteKeys) {
        dirtyMap.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>()).compute(cacheKey, (key, old) -> {
            if (old == null) {
                return new DirtyEntry(tableName, cacheKey, primaryRouteId, op, snapshot, deleteKeys);
            }
            old.mergeOp(op, snapshot, deleteKeys);
            return old;
        });
    }

    public boolean isDirty(String tableName, String cacheKey) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        return tableDirty != null && tableDirty.containsKey(cacheKey);
    }

    public DirtyEntry get(String tableName, String cacheKey) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        if (tableDirty == null) {
            return null;
        }
        return tableDirty.get(cacheKey);
    }

    public void remove(String tableName, String cacheKey) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        if (tableDirty != null) {
            tableDirty.remove(cacheKey);
            if (tableDirty.isEmpty()) {
                dirtyMap.remove(tableName);
            }
        }
    }

    public List<DirtyEntry> listByTable(String tableName) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        if (tableDirty == null || tableDirty.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(tableDirty.values());
    }

    /**
     * 按 routeId 查询脏数据
     */
    public List<DirtyEntry> listByRouteId(long routeId) {
        List<DirtyEntry> result = new ArrayList<>();
        for (Map<String, DirtyEntry> tableDirty : dirtyMap.values()) {
            for (DirtyEntry entry : tableDirty.values()) {
                if (entry.getPrimaryRouteId() == routeId) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * 按表 + routeId 查询脏数据
     */
    public List<DirtyEntry> listByTableAndRouteId(String tableName, long routeId) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        if (tableDirty == null || tableDirty.isEmpty()) {
            return List.of();
        }
        List<DirtyEntry> result = new ArrayList<>();
        for (DirtyEntry entry : tableDirty.values()) {
            if (entry.getPrimaryRouteId() == routeId) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 按分片查询脏数据（routeId % portionSize == portion）
     */
    public List<DirtyEntry> listByTableAndPortion(String tableName, int portion, int portionSize) {
        Map<String, DirtyEntry> tableDirty = dirtyMap.get(tableName);
        if (tableDirty == null || tableDirty.isEmpty()) {
            return List.of();
        }
        List<DirtyEntry> result = new ArrayList<>();
        for (DirtyEntry entry : tableDirty.values()) {
            if (Math.floorMod(entry.getPrimaryRouteId(), portionSize) == portion) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<DirtyEntry> listAll() {
        List<DirtyEntry> all = new ArrayList<>();
        for (Map<String, DirtyEntry> tableDirty : dirtyMap.values()) {
            all.addAll(tableDirty.values());
        }
        return all;
    }
}
