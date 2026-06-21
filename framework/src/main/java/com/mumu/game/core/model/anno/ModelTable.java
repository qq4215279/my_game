package com.mumu.game.core.model.anno;

import com.mumu.game.core.model.consts.ModelConstants;
import com.mumu.game.core.model.consts.PersistStrategy;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.redis.constants.SerializerType;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ModelTable
 * 游戏表注解
 * @author liuzhen
 * @version 1.0.0 2026/6/21 14:18
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelTable {
    /** 表名 */
    String name();

    /** 描述 */
    String comment() default StringUtils.EMPTY;

    /** 数据所属服务（NONE 默认表示都不属于，非 JVM/SHARD 缓存不要用 ALL） */
    ServiceType[] belongs() default ServiceType.NONE;

    /** 数据持久化策略（默认JVM） */
    PersistStrategy persistStrategy() default PersistStrategy.JVM;

    // ============================= 扩展设置 ================================

    /** 【非JVM策略参数】是否为共享数据模型（此模型允许任意服读写，只提供多服的数据缓存和刷新，需业务自己维护并发写） */
    boolean shared() default false;

    /** 跳过模型操作的线程检查 */
    boolean uncheck() default false;

    /** 【非JVM策略参数】缓存数据预加载（true-玩家进入服务器时就加载数据，false-玩家在第一次获取缓存模型时加载数据） */
    boolean preLoad() default false;

    /** 数据容量限制，超出时移除最久的元素（需要保证数据升序，且新增的数据一定比历史数据大） */
    int capacity() default Integer.MAX_VALUE;

    /** 【非JVM策略参数】数据持久化间隔 ms（不频繁修改的表，可设置久一点） */
    int persistInterval() default ModelConstants.PERSIST_INTERVAL;

    /** 【Redis策略参数】redis过期时间 s */
    int expire() default ModelConstants.REDIS_MODEL_EXPIRE;

    /**
     * TODO【MongoDB 索引】 待实现！！！
     * 如果没有定义：
     * 1. 先使用AutoColumn注解中定义的主键列表。
     * 2. 如果是非DB数据持久化策略，且是SELECT_ONE策略，则默认玩家id是主键(playerId) TODO 玩家id 字段命令统一为 playerId
     */
    Index[] indexes() default {};

    /** 编码类型 - 数据打包传递时使用 */
    SerializerType serializerType() default SerializerType.JSON;
}
