package com.mumu.game.core.db.dirty;

import com.mumu.game.core.db.consts.PersistOp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DirtyEntry
 * 脏数据条目
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Data
@AllArgsConstructor
public final class DirtyEntry {

    /** 表名 */
    private final String tableName;
    /** 脏数据 cacheKey */
    private final String cacheKey;
    /** 路由分片 id */
    private final long routeId;
    /** 待持久化操作类型 */
    private PersistOp op;

    /**
     * 合并操作类型（多次 update 仍视为 update）
     */
    public void mergeOp(PersistOp newOp) {
        this.op = this.op.merge(newOp);
    }
}
