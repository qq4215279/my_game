package com.mumu.game.core.db.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang3.StringUtils;

import com.mumu.game.core.db.consts.ModelConstants;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.redis.constants.SerializerType;

/**
 * ModelTable
 * 游戏实体表注解
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

    /** 数据持久化策略（默认JVM） */
    PersistStrategy persistStrategy() default PersistStrategy.JVM;

    /** 索引数组 */
    Index[] indexes() default {};

    // ============================= 扩展设置 ================================

    /** 【非JVM策略参数】缓存数据预加载（true-玩家进入服务器时就加载数据，false-玩家在第一次获取缓存模型时加载数据） */
    boolean preLoad() default true;

    /** 跳过写操作线程校验（全局表由业务层 Redis 锁保证并发） 非用户玩家表标识！ */
    boolean skipThreadCheck() default false;

    /** 数据容量限制，超出时移除最久的元素（需要保证数据升序，且新增的数据一定比历史数据大） */
    int capacity() default Integer.MAX_VALUE;

    /** 【非JVM策略参数】数据持久化间隔 ms（不频繁修改的表，可设置久一点） */
    int persistInterval() default ModelConstants.PERSIST_INTERVAL;

    /** 【Redis策略参数】redis过期时间 s */
    int expire() default ModelConstants.REDIS_MODEL_EXPIRE;

    /** 编码类型 - 数据打包传递时使用 */
    SerializerType serializerType() default SerializerType.JSON;
}
