package com.mumu.game.core.db.core;

/**
 * BaseEntity
 * 抽象领域实体
 * @author liuzhen
 * @version 1.0.0 2026/7/9 16:57
 */
public abstract class BaseEntity {

    /**
     * 获取主索引routeId
     * @return long
     * @since 2026/7/10 17:05
     */
    public abstract long getPrimaryRouteId();

    /** 序列化 会在数据初始化的时候调用 */
    public void marshal() {}

    /** 反序列化 会在数据持久化或数据打包的时候调用 */
    public void unmarshal() {}


}
