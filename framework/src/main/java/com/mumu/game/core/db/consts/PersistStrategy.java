package com.mumu.game.core.db.consts;

/**
 * PersistStrategy
 * 数据持久化策略
 * @author liuzhen
 * @version 1.0.0 2026/6/21 14:49
 */
public enum PersistStrategy {
    /** 仅java内存 */
    JVM,
    /** 仅数据库 */
    DB,
    /** java内存 + Redis2级缓存 */
    REDIS,
    /** java内存 + Redis + DB持久化 */
    REDIS_DB

    ;


}
