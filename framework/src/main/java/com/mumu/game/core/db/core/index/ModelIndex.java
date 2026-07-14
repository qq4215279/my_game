package com.mumu.game.core.db.core.index;

import java.util.Collection;
import java.util.List;

import com.mumu.game.core.db.core.BaseEntity;

/**
 * ModelIndex
 * 路由桶内索引统一抽象（主索引 / 副索引）
 * 只存实体引用，不拷贝对象
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
interface ModelIndex<Entity extends BaseEntity> {

    /**
     * 写入索引
     * @return 同 key 下被替换的旧实体；多值副索引通常返回 null
     */
    Entity put(String indexKey, Entity entity);

    /**
     * 按 key + 实体引用移除（副索引按引用匹配；主索引可按 key 删除）
     */
    void remove(String indexKey, Entity entity);

    /** 完整键查询单条（非唯一索引取第一条） */
    Entity getOne(String indexKey);

    /** 完整键查询全部 */
    List<Entity> getAll(String indexKey);

    /**
     * 左前缀查询（扫索引 key 的 startsWith）
     * <p>主索引左前缀业务查询仍建议用 {@code IndexMeta#matchPrefix} 扫 values</p>
     */
    List<Entity> leftFind(String prefix);

    boolean isEmpty();

    int size();

    /** 桶内全部实体（主索引扫桶 / route 淘汰快照） */
    Collection<Entity> values();
}
