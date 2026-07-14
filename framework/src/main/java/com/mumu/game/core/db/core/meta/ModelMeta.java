package com.mumu.game.core.db.core.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.consts.PersistStrategy;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.util.ReflectFieldUtil;
import com.mumu.game.core.redis.constants.RedisKey;
import com.mumu.game.core.redis.constants.SerializerType;
import com.mumu.game.expcetion.ModelArgException;

import lombok.Getter;

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
@Getter
public final class ModelMeta {

    /** 实体类 */
    private final Class<?> entityClass;
    /** 表名（与 @ModelTable.name 一致） */
    private final String tableName;
    /** 表描述 */
    private final String comment;
    /** 持久化策略（JVM / REDIS / DB / REDIS_DB） */
    private final PersistStrategy persistStrategy;
    // 索引信息
    /** 主索引（indexes[0]，必须唯一） */
    private final IndexMeta primaryIndex;
    /** 非主索引列表（启动期从 indexes 过滤，JVM 副索引维护用） */
    private final List<IndexMeta> secondaryIndexes;
    /** 路由字段名（主索引第一个字段） 用于：业务线程路由、Redis MANY 模式分桶、dirty 重试路由 */
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
        this.entityClass = builder.entityClass;
        this.tableName = builder.tableName;
        this.comment = builder.comment;
        this.persistStrategy = builder.persistStrategy;

        // 索引信息
        // 主索引
        List<IndexMeta> indexes = Collections.unmodifiableList(builder.indexes);
        this.primaryIndex = builder.indexes.getFirst();
        this.routeField = primaryIndex.getFields()[0];
        this.singleFieldPrimary = primaryIndex.getFields().length == 1;
        // 副索引
        List<IndexMeta> secondary = new ArrayList<>();
        for (IndexMeta index : indexes) {
            if (!index.isPrimary()) {
                secondary.add(index);
            }
        }
        this.secondaryIndexes = Collections.unmodifiableList(secondary);

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
     * @param entityClass   实体类
     * @param table         @ModelTable 注解
     * @param persistEngine 持久化引擎名
     */
    public static <Entity extends BaseEntity> ModelMeta parse(Class<Entity> entityClass, ModelTable table, String persistEngine) {
        Builder builder = new Builder();
        builder.entityClass = entityClass;
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
            builder.indexes.add(IndexMeta.from(entityClass, tableIndexes[i], i == 0));
        }
        return builder.build();
    }

    /**
     * 按名称获取索引
     * @param indexName 索引名
     */
    public IndexMeta getIndex(String indexName) {
        if (primaryIndex.getName().equals(indexName)) {
            return primaryIndex;
        }

        for (IndexMeta index : secondaryIndexes) {
            if (index.getName().equals(indexName)) {
                return index;
            }
        }
        throw new ModelArgException("表 " + tableName + " 不存在索引: " + indexName);
    }

    /** 是否使用 JVM 缓存 */
    public boolean hasJVM() {
        return persistStrategy != PersistStrategy.DB;
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
    @SuppressWarnings("all")
    public boolean isPlayerScoped() {
        return !skipThreadCheck;
    }

    /** 是否存在非主索引（需要维护 JVM 副索引） */
    @SuppressWarnings("all")
    public boolean hasSecondaryIndex() {
        return !secondaryIndexes.isEmpty();
    }

    /**
     * 从查询键提取路由分片 id（取 keys 第一个值）
     */
    public long getRouteId(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new ModelArgException("路由键不能为空");
        }
        Object value = keys[0];
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 构建 Redis Hash Key
     * <p>单字段主键：model:{tableName}；复合主键：model:{tableName}:{primaryRouteId}</p>
     */
    public String buildRedisKey(long primaryRouteId) {
        if (singleFieldPrimary) {
            return RedisKey.AUTO_MODEL_CACHE_ONE.buildKey(tableName);
        }
        return RedisKey.AUTO_MODEL_CACHE_MANY.buildKey(tableName, primaryRouteId);
    }

    public String buildRedisKey(BaseEntity entity) {
        return buildRedisKey(entity.getPrimaryRouteId());
    }

    /**
     * 构建 Redis Hash Field
     */
    public String buildHashField(BaseEntity entity, IndexMeta index) {
        Object[] values = index.readKeyValues(entity);
        if (index == primaryIndex) {
            if (singleFieldPrimary) {
                return String.valueOf(values[0]);
            } else {
                return buildCompositeField(values, 1, values.length);
            }
        }

        return buildCompositeField(values, 0, values.length);
    }

    public String buildHashField(IndexMeta index, Object... keys) {
        if (index == primaryIndex) {
            if (singleFieldPrimary) {
                return String.valueOf(keys[0]);
            } else {
                return buildCompositeField(keys, 1, keys.length);
            }
        }

        return index.buildKeyString(keys);
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




    /** 内部构建器，build  */
    private static final class Builder {
        private Class<?> entityClass;
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

        /** 构建模型元数据 */
        private ModelMeta build() {
            // 1. 校验
            validate(this.entityClass, this.indexes);
            // 2. 创建
            return new ModelMeta(this);
        }

        /** 模型元数据启动校验 */
        private void validate(Class<?> domainClass, List<IndexMeta> indexes) {
            if (!BaseEntity.class.isAssignableFrom(domainClass)) {
                throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 必须继承 BaseEntity");
            }
            if (indexes == null || indexes.isEmpty()) {
                throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 未定义 @ModelTable.indexes");
            }
            IndexMeta primary = indexes.get(0);
            if (!primary.isUnique()) {
                throw new IllegalStateException(
                        "实体 " + domainClass.getSimpleName() + " 第一个索引必须是唯一索引: " + primary.getName());
            }
            if (!primary.isPrimary()) {
                throw new IllegalStateException("实体 " + domainClass.getSimpleName() + " 第一个索引必须标记为主索引");
            }
            for (IndexMeta index : indexes) {
                for (String field : index.getFields()) {
                    if (!ReflectFieldUtil.hasField(domainClass, field)) {
                        throw new IllegalStateException(
                                "实体 " + domainClass.getSimpleName() + " 索引字段不存在: " + field);
                    }
                }
            }
        }
    }
}
