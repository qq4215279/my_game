package com.mumu.game.core.db.consts;

/**
 * ModelConstants
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/21 14:19
 */
public interface ModelConstants {
    /** 默认缓存天数（JVM route 桶 expireAfterAccess） */
    int CACHE_DAY = 1;

    /** 默认 route 桶数量上限（JVM Guava Cache maximumSize） */
    int CACHE_SIZE = 200000;

    /** 批量同步比例 */
    int EVENT_SYN_RATIO = 5;

    /** 持久化存储间隔（ms） */
    int PERSIST_INTERVAL = 2000;

    /** 持久化批量存储条数（每次持久化的数量） */
    int PERSIST_BATCH_SIZE = 3000;

    /** 数据库批量写入数量（实际数据库写入参数） */
    int DB_BATCH_SIZE = 1000;

    /** Redis缓存批量操作数据量（实际Redis的写入参数） */
    int REDIS_BATCH_SIZE = 500;

    /** Redis中数据缓存过期时间 */
    int REDIS_MODEL_EXPIRE = CACHE_DAY * 24 * 60 * 60;
}
