package com.mumu.game.core.db.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.redis.constants.RedisKey;
import com.mumu.game.core.redis.constants.SerializerType;

import lombok.Data;

/**
 * ModelMeta
 * 表模型运行时元数据，由 {@link ModelTable} 注解在启动期解析生成。
 * <p>
 * 职责：
 * <ul>
 *   <li>承载表级配置（持久化策略、容量、预加载、引擎类型等）</li>
 *   <li>构建 Redis Key / Hash Field / JVM cacheKey</li>
 *   <li>提供路由分片 id（主索引第一个字段）</li>
 * </ul>
 * <p>
 * Redis 分桶示例：
 * <ul>
 *   <li>Player（单字段主键 playerId）→ key: model:player, field: playerId</li>
 *   <li>PlayerTemplate（playerId+functionId）→ key: model:player_template:{playerId}, field: functionId</li>
 * </ul>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Data
public final class ModelMeta {

    /** 实体类 */
    private final Class<?> entityClass;
    /** 表名（与 @ModelTable.name 一致） */
    private final String tableName;
    /** 表描述 */
    private final String comment;
    /** 持久化策略（JVM / REDIS / DB / REDIS_DB） */
    private final PersistStrategy persistStrategy;
    /** 全部索引（第一个为主索引） */
    private final List<IndexMeta> indexes;
    /** 主索引（indexes[0]，必须唯一） */
    private final IndexMeta primaryIndex;
    /**
     * 路由字段名（主索引第一个字段）
     * <p>用于：业务线程路由、Redis MANY 模式分桶、dirty 重试路由</p>
     */
    private final String routeField;
    /** 主索引是否仅由一个字段构成（决定 Redis ONE / MANY 模式） */
    private final boolean singleFieldPrimary;
    /** 是否进服预加载 */
    private final boolean preLoad;
    /** JVM 每桶最大记录数（超出按插入序淘汰最久元素） */
    private final int capacity;
    /** 定时落库间隔（ms） */
    private final int persistInterval;
    /** Redis 过期时间（s） */
    private final int expire;
    /** 序列化方式（Redis 存取、打包传递） */
    private final SerializerType serializerType;
    /** 持久化引擎类型（mongo / mysql 等，默认 mongo） */
    private final String persistEngine;
    /** 非玩家维度表：true 时跳过线程校验，且不参与玩家进服预加载/下线 flush */
    private final boolean skipThreadCheck;


    private ModelMeta(Builder builder) {
        this.entityClass = builder.domainClass;
        this.tableName = builder.tableName;
        this.comment = builder.comment;
        this.persistStrategy = builder.persistStrategy;
        this.indexes = Collections.unmodifiableList(builder.indexes);
        this.primaryIndex = builder.indexes.getFirst();
        this.routeField = primaryIndex.getFields()[0];
        this.singleFieldPrimary = primaryIndex.getFields().length == 1;
        this.preLoad = builder.preLoad;
        this.capacity = builder.capacity;
        this.persistInterval = builder.persistInterval;
        this.expire = builder.expire;
        this.serializerType = builder.serializerType;
        this.persistEngine = builder.persistEngine;
        this.skipThreadCheck = builder.skipThreadCheck;
    }

    /**
     * 从实体类与注解解析表元数据
     *
     * @param domainClass   实体类
     * @param table         @ModelTable 注解
     * @param persistEngine 持久化引擎名
     */
    public static ModelMeta parse(Class<?> domainClass, ModelTable table, String persistEngine) {
        Builder builder = new Builder();
        builder.domainClass = domainClass;
        builder.tableName = table.name();
        builder.comment = table.comment();
        builder.persistStrategy = table.persistStrategy();
        builder.preLoad = table.preLoad();
        builder.capacity = table.capacity();
        builder.persistInterval = table.persistInterval();
        builder.expire = table.expire();
        builder.serializerType = table.serializerType();
        builder.persistEngine = persistEngine;
        builder.skipThreadCheck = table.skipThreadCheck();
        com.mumu.game.core.db.anno.Index[] tableIndexes = table.indexes();
        for (int i = 0; i < tableIndexes.length; i++) {
            builder.indexes.add(IndexMeta.from(domainClass, tableIndexes[i], i == 0));
        }
        return builder.build();
    }

    /**
     * 按名称获取索引
     *
     * @param indexName 索引名
     */
    public IndexMeta getIndex(String indexName) {
        for (IndexMeta index : indexes) {
            if (index.getName().equals(indexName)) {
                return index;
            }
        }
        throw new IllegalArgumentException("表 " + tableName + " 不存在索引: " + indexName);
    }

    /** 是否使用 Redis 二级缓存 */
    public boolean hasRedis() {
        return persistStrategy == PersistStrategy.REDIS || persistStrategy == PersistStrategy.REDIS_DB;
    }

    /** 是否落库到 DB */
    public boolean hasDb() {
        return persistStrategy == PersistStrategy.DB || persistStrategy == PersistStrategy.REDIS_DB;
    }

    /**
     * 是否为玩家维度表（参与进服预加载 / 下线 flush）
     * <p>{@code skipThreadCheck=true} 表示非玩家表，生命周期与线程校验均跳过</p>
     */
    public boolean isPlayerScoped() {
        return !skipThreadCheck;
    }

    /**
     * 从实体提取路由分片 id（走 {@link BaseEntity#getPrimaryRouteId()}，无反射）
     */
    public long getRouteId(BaseEntity entity) {
        return entity.getPrimaryRouteId();
    }

    /**
     * 从查询键提取路由分片 id（取 keys 第一个值）
     */
    public long getRouteId(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("路由键不能为空");
        }
        Object value = keys[0];
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 构建 Redis Hash Key
     * <p>单字段主键：model:{tableName}；复合主键：model:{tableName}:{routeId}</p>
     */
    public String buildRedisKey(long routeId) {
        if (singleFieldPrimary) {
            return RedisKey.AUTO_MODEL_CACHE_ONE.buildKey(tableName);
        }
        return RedisKey.AUTO_MODEL_CACHE_MANY.buildKey(tableName, routeId);
    }

    public String buildRedisKey(BaseEntity entity) {
        return buildRedisKey(getRouteId(entity));
    }

    /**
     * 构建 Redis Hash Field
     */
    public String buildHashField(BaseEntity entity, IndexMeta index) {
        Object[] values = index.readKeyValues(entity);
        if (singleFieldPrimary && index == primaryIndex) {
            return String.valueOf(values[0]);
        }
        if (index == primaryIndex && !singleFieldPrimary) {
            return buildCompositeField(values, 1, values.length);
        }
        return buildCompositeField(values, 0, values.length);
    }

    public String buildHashField(IndexMeta index, Object... keys) {
        if (singleFieldPrimary && index == primaryIndex) {
            return String.valueOf(keys[0]);
        }
        if (index == primaryIndex && !singleFieldPrimary) {
            return buildCompositeField(keys, 1, keys.length);
        }
        return index.buildKeyString(keys);
    }

    /**
     * 构建 JVM / dirty 唯一 cacheKey
     * <p>格式：{tableName}:{indexName}:{key1:key2...}</p>
     */
    public String buildCacheKey(BaseEntity entity) {
        return buildCacheKey(primaryIndex, primaryIndex.readKeyValues(entity));
    }

    public String buildCacheKey(IndexMeta index, Object... keys) {
        return tableName + ':' + index.getName() + ':' + index.buildKeyString(keys);
    }

    /**
     * 拼接复合 field（用冒号连接）
     */
    private static String buildCompositeField(Object[] values, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(':');
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /** 内部构建器，build 前执行 {@link ModelMetaValidator} 校验 */
    private static final class Builder {
        private Class<?> domainClass;
        private String tableName;
        private String comment;
        private PersistStrategy persistStrategy;
        private final List<IndexMeta> indexes = new ArrayList<>();
        private boolean preLoad;
        private int capacity;
        private int persistInterval;
        private int expire;
        private SerializerType serializerType;
        private String persistEngine;
        private boolean skipThreadCheck;

        private ModelMeta build() {
            ModelMetaValidator.validate(this.domainClass, this.indexes);
            return new ModelMeta(this);
        }
    }
}
