package com.mumu.game.core.db.anno;

/**
 * Index
 * 索引
 * @author liuzhen
 * @version 1.0.0 2026/6/21 14:20
 */
public @interface Index {

    /**
     * 索引名称，不填，默认为：unq_字段名1 / idx_字段名2_字段名3
     */
    String name() default "";

    /**
     * 索引列, 索引列为字段名
     */
    String[] value();

    /**
     * 是否是唯一索引
     */
    boolean unique() default true;
}
