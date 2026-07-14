package com.mumu.game.core.db.core.dirty;

import com.mumu.game.core.db.consts.PersistOp;
import com.mumu.game.core.db.core.BaseEntity;

import lombok.Data;

/**
 * DirtyEntry
 * 脏数据条目
 * 有 JVM 时通常只记 cacheKey + op，flush 时从内存取实体；
 * 仅 DB（无 JVM）时需携带 {@link #snapshot} / {@link #deleteKeys}，flush 不依赖内存。
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Data
public final class DirtyEntry {

    /** 表名 */
    private final String tableName;
    /** 脏数据 cacheKey */
    private final String cacheKey;
    /** 路由分片 id */
    private final long primaryRouteId;
    /** 待持久化操作类型 */
    private PersistOp op;
    /** upsert 用实体快照（仅 DB 等无 JVM 场景） */
    private BaseEntity snapshot;
    /** 删除主键（可选；缺省可由 cacheKey 解析） */
    private Object[] deleteKeys;

    public DirtyEntry(String tableName, String cacheKey, long primaryRouteId, PersistOp op) {
        this(tableName, cacheKey, primaryRouteId, op, null, null);
    }

    public DirtyEntry(String tableName, String cacheKey, long primaryRouteId, PersistOp op,
                      BaseEntity snapshot, Object[] deleteKeys) {
        this.tableName = tableName;
        this.cacheKey = cacheKey;
        this.primaryRouteId = primaryRouteId;
        this.op = op;
        this.snapshot = snapshot;
        this.deleteKeys = deleteKeys;
    }

    /**
     * 合并操作类型（多次 update 仍视为 update）
     */
    public void mergeOp(PersistOp newOp) {
        mergeOp(newOp, null, null);
    }

    /**
     * 合并操作，并按结果更新快照/删除键
     */
    public void mergeOp(PersistOp newOp, BaseEntity newSnapshot, Object[] newDeleteKeys) {
        this.op = this.op.merge(newOp);
        if (this.op == PersistOp.DELETE) {
            this.snapshot = null;
            if (newDeleteKeys != null) {
                this.deleteKeys = newDeleteKeys;
            }
        } else if (newSnapshot != null) {
            this.snapshot = newSnapshot;
            this.deleteKeys = null;
        }
    }
}
