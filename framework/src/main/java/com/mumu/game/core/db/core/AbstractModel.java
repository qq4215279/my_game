package com.mumu.game.core.db.core;

import java.util.List;
import java.util.function.Supplier;

/**
 * AbstractModel
 * 数据model抽象
 * @author liuzhen
 * @version 1.0.0 2026/7/9 16:59
 */
public interface AbstractModel<T extends AbstractDomain> {

    void insert(T t);

    /**
     *
     * @param t t
     * @param persistNow 立马插入数据
     */
    void insert(T t, boolean persistNow);

    void insertOrUpdate(T t);
    

    void delete(T t);
    void delete(T t, boolean persistNow);

    void deleteOne(Object... primaryKeys);

    void deleteOne(boolean persistNow, Object... primaryKeys);
    void deleteOne(String indexName, boolean persistNow, Object... primaryKeys);

    void deleteAll(Object... primaryKeys);
    void deleteAll(String indexName, Object... primaryKeys);
    void deleteAll(String indexName, boolean persistNow, Object... primaryKeys);

    T selectOne(Object... primaryKeys);
    T selectOne(String indexName, Object... primaryKeys);

    List<T> selectList(Object... primaryKeys);

    List<T> selectList(String indexName, Object... primaryKeys);

    /** 获取满足条件的缓存数据（倒序） */
    List<T> selectListReverse(Object... primaryKeys);

    List<T> selectListReverse(String indexName, Object... primaryKeys);

    default T selectOrCreate(Supplier<T> builder, Object... primaryKeys) {
        return selectOrCreate(builder, false, primaryKeys);
    }

    default T selectOrCreate(Supplier<T> builder, boolean persistNow, Object... primaryKeys) {
        T t = selectOne(primaryKeys);
        if (t == null) {
            t = builder.get();
            insert(t, persistNow);
        }
        return t;
    }
    default T selectOrCreate(Supplier<T> builder, boolean persistNow, String indexName, Object... primaryKeys) {
        T t = selectOne(indexName, primaryKeys);
        if (t == null) {
            t = builder.get();
            insert(t, persistNow);
        }
        return t;
    }

    void update(T t);

    void update(T t, boolean persistNow);

}
