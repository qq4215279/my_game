package com.mumu.game.core.db.consts;

/**
 * PersistOp
 * 持久化操作类型
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public enum PersistOp {
    /** 新增 */
    INSERT,
    /** 更新 */
    UPDATE,
    /** 删除 */
    DELETE,
    ;

    /**
     * 合并操作类型（多次 update 仍视为 update）
     */
    public PersistOp merge(PersistOp other) {
        if (this == DELETE || other == DELETE) {
            return DELETE;
        }
        if (this == INSERT || other == INSERT) {
            return INSERT;
        }
        return UPDATE;
    }
}
