package com.mumu.game.core.db.core.index;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;

/**
 * SecondaryIndex
 * 路由桶内非主索引标记接口
 * @author liuzhen
 * @version 1.0.0 2026/7/14
 */
public interface SecondaryIndex<Entity extends BaseEntity> extends ModelIndex<Entity> {

    /** 按索引元数据创建唯一 / 非唯一副索引 */
    static <Entity extends BaseEntity> SecondaryIndex<Entity> create(IndexMeta index) {
        return index.isUnique() ? new UniqueSecondaryIndex<>() : new MultiSecondaryIndex<>();
    }
}
