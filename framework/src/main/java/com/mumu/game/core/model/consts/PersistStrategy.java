package com.mumu.game.core.model.consts;

/**
 * PersistStrategy
 * 数据持久化策略
 * @author liuzhen
 * @version 1.0.0 2026/6/21 14:49
 */
public enum PersistStrategy {
    /** java内存 */
    JVM,
    /** 数据库 */
    DB,
    /** Redis */
    REDIS,
    /** Redis和DB */
    REDIS_DB
    ;

    /** 是否属于DB相关策略 */
    public static boolean belongsToDB(PersistStrategy strategy) {
        return strategy == DB || strategy == REDIS_DB;
    }
}
