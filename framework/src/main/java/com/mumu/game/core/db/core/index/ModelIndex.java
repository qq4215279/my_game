package com.mumu.game.core.db.core.index;

import java.util.Collection;
import java.util.List;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;

/**
 * ModelIndex
 * 路由桶内索引统一抽象（主索引 / 副索引）
 * 只存实体引用，不拷贝对象
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
interface ModelIndex<Entity extends BaseEntity> {

    /** 完整键查询单条（非唯一索引取第一条） */
    Entity getOne(String indexKey);

    /** 完整键查询全部 */
    List<Entity> getAll(String indexKey);

    /**
     * 写入索引
     * @return 同 key 下被替换的旧实体；多值副索引通常返回 null
     */
    Entity put(String indexKey, Entity entity);

    /**
     * 按 key + 实体引用移除（副索引按引用匹配；主索引可按 key 删除）
     */
    void remove(String indexKey, Entity entity);

    /**
     * 左前缀查询
     * <p>主索引：{@link IndexMeta#matchPrefix} 扫桶内实体；副索引：indexKey startsWith</p>
     */
    List<Entity> leftFind(IndexMeta index, Object... keys);

    /**
     * 是否为空
     * @return boolean
     */
    boolean isEmpty();

    /**
     * 数量
     * @return int
     */
    int size();

    /**
     * 获取桶内全部实体（主索引扫桶 / route 淘汰快照）
     * @return java.util.Collection<Entity>
     */
    Collection<Entity> values();
}
