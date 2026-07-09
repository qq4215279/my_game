package com.mumu.game.core.db.core;

/**
 * AbstractDomain
 * 抽象领域实体
 * @author liuzhen
 * @version 1.0.0 2026/7/9 16:57
 */
public abstract class AbstractDomain {

    /** 序列化 会在数据初始化的时候调用 */
    public void marshal() {}

    /** 反序列化 会在数据持久化或数据打包的时候调用 */
    public void unmarshal() {}

}
