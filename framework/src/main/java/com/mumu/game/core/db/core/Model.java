package com.mumu.game.core.db.core;

import java.util.List;
import java.util.function.Supplier;

/**
 * Model
 * 数据模型对外 API，定义缓存实体的查询与写操作语义。
 * 读路径（select 系列）：
 *   {@code PersistStrategy.DB}（无 JVM）：每次直查持久引擎，不回填、不预加载。
 *   有 JVM 的策略：按 primaryRouteId 分片加载——若尚未加载则整桶从 L2 Redis 拉取，空则再从 L3 DB
 *   {@code findList(routeId)} 回填 L1（并可回写 L2）；无论有无数据都标记「已加载」（空桶表示确认无数据），后续只查 L1。
 *   {@code preLoad=true} 且有 JVM 时进服提前 {@code ensureRouteLoaded}；否则首次 select 懒加载。
 * 写路径（insert / update / delete）：
 *   仅 DB：默认异步（dirty 携带实体快照/删除键后 flush）；{@code persistNow = true} 时同步写引擎。
 *   有 JVM：默认异步落库（标记 dirty 后由持久化线程 flush）；{@code persistNow = true} 时同步 flush 到 Redis / DB。
 * 写操作约束：
 *   有 JVM 时 update / delete(entity) 要求传入对象与 JVM 缓存中为同一引用
 *   写操作需在对应 routeId 的业务线程执行（全局表可通过 {@code skipThreadCheck} 跳过）
 * 异常策略：实现侧（{@code BaseModel}）对入口做 try-catch，失败打 MODEL 错误日志；
 *   读失败返回 {@code null}/空列表，写失败直接返回，避免打断业务线程。
 * @param <Entity> 实体类型
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public interface Model<Entity extends BaseEntity> {

    /**
     * 按主索引完整键查询单条记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * @param primaryKeys 主索引完整键（键数量须与主索引字段数一致）
     * @return 实体对象，不存在或失败返回 {@code null}
     */
    Entity selectOne(Object... primaryKeys);

    /**
     * 按指定索引完整键查询单条记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * @param primaryRouteId   主索引路由id（路由桶id）
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param secondaryKeys 索引完整键（键数量须与该索引字段数一致）
     * @return 实体对象，不存在或失败返回 {@code null}
     */
    Entity selectOne(long primaryRouteId, String indexName, Object... secondaryKeys);

    /**
     * 查询单条记录，不存在则创建并插入。
     * 默认异步持久化，等价于 {@code selectOrCreate(builder, false, primaryKeys)}。
     * @param builder     记录不存在时的实体构造器
     * @param primaryKeys 主索引完整键
     * @return 已存在或新创建的实体
     */
    default Entity selectOrCreate(Supplier<Entity> builder, Object... primaryKeys) {
        return selectOrCreate(builder, false, primaryKeys);
    }

    /**
     * 查询单条记录，不存在则创建并插入。
     * @param builder     记录不存在时的实体构造器
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param primaryKeys 主索引完整键
     * @return 已存在或新创建的实体
     */
    default Entity selectOrCreate(Supplier<Entity> builder, boolean persistNow, Object... primaryKeys) {
        Entity entity = selectOne(primaryKeys);
        if (entity == null) {
            entity = builder.get();
            insert(entity, persistNow);
        }
        return entity;
    }

    /**
     * 按指定索引查询单条记录，不存在则创建并插入。
     * @param builder     记录不存在时的实体构造器
     * @param primaryRouteId   主索引路由id（路由桶id）
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param secondaryKeys 索引完整键
     * @return 已存在或新创建的实体
     */
    default Entity selectOrCreate(Supplier<Entity> builder, long primaryRouteId, boolean persistNow, String indexName, Object... secondaryKeys) {
        Entity entity = selectOne(primaryRouteId, indexName, secondaryKeys);
        if (entity == null) {
            entity = builder.get();
            insert(entity, persistNow);
        }
        return entity;
    }

    /**
     * 按主索引键查询列表。
     * 完整键时等价于 {@code selectOne} 的单条结果；
     * 左前缀键（键数量小于索引字段数）时返回该前缀下所有匹配记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 匹配列表，无数据返回空列表
     */
    List<Entity> selectList(Object... primaryKeys);

    /**
     * 按指定索引键查询列表。
     * 完整键时等价于 {@code selectOne} 的单条结果；
     * 左前缀键时返回该前缀下所有匹配记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * @param primaryRouteId   主索引路由id（路由桶id）
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param secondaryKeys 索引键（完整键或左前缀）
     * @return 匹配列表，无数据返回空列表
     */
    List<Entity> selectList(long primaryRouteId, String indexName, Object... secondaryKeys);

    /**
     * 按主索引键查询列表，结果按 cacheKey 倒序排列。
     * 语义同 {@link #selectList(Object...)}，仅排序方式不同。
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 倒序匹配列表，无数据返回空列表
     */
    List<Entity> selectListReverse(Object... primaryKeys);

    /**
     * 按指定索引键查询列表，结果按 cacheKey 倒序排列。
     * @param primaryRouteId   主索引路由id（路由桶id）
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param secondaryKeys 索引键（完整键或左前缀）
     * @return 倒序匹配列表，无数据返回空列表
     */
    List<Entity> selectListReverse(long primaryRouteId, String indexName, Object... secondaryKeys);

    /**
     * 更新实体（异步持久化）。
     * 实体须已存在于 JVM 缓存，且传入对象与缓存中为同一引用。
     * 多次 update 同一记录仅标记一次 dirty，flush 时读取 JVM 最新对象。
     * @param entity 待更新实体（须为 JVM 缓存中的同一引用）
     */
    void update(Entity entity);

    /**
     * 更新实体。
     * @param entity     待更新实体（须为 JVM 缓存中的同一引用）
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     */
    void update(Entity entity, boolean persistNow);

    /**
     * 插入新记录（异步持久化）。
     * 记录已存在时记录错误日志并返回；写入 JVM 缓存并标记 dirty。
     * @param entity 待插入实体
     */
    void insert(Entity entity);

    /**
     * 插入新记录。
     * @param entity     待插入实体
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     */
    void insert(Entity entity, boolean persistNow);

    /**
     * 插入或更新（异步持久化）。
     * 记录不存在时执行 insert；已存在时执行 update。
     * 存在性判断路径：L1 JVM → L2 Redis → L3 DB，命中后回填上层缓存。
     * update 分支要求传入对象与 JVM 缓存中为同一引用。
     * @param entity 待写入实体
     */
    void insertOrUpdate(Entity entity);

    /**
     * 按实体对象删除（异步持久化）。
     * 从 JVM 缓存移除记录并标记 DELETE dirty。
     * 若缓存中仍有该记录，传入对象须与缓存中为同一引用。
     * @param entity 待删除实体
     */
    void delete(Entity entity);

    /**
     * 按实体对象删除。
     * @param entity     待删除实体
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     */
    void delete(Entity entity, boolean persistNow);

    /**
     * 按主索引完整键删除单条记录（异步持久化）。
     * 无需传入实体对象，直接从 JVM 缓存移除并标记 DELETE dirty。
     * @param primaryKeys 主索引完整键
     */
    void deleteOne(Object... primaryKeys);

    /**
     * 按主索引完整键删除单条记录。
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param primaryKeys 主索引完整键
     */
    void deleteOne(boolean persistNow, Object... primaryKeys);

    /**
     * 按主索引左前缀批量删除（异步持久化）。
     * 删除 JVM 缓存中该前缀下所有匹配记录，并逐条标记 DELETE dirty。
     * @param primaryKeys 索引键（左前缀，至少包含 routeId）
     */
    void deleteAll(Object... primaryKeys);

    /**
     * 按指定索引左前缀批量删除（异步持久化）。
     * @param persistNow   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引键（左前缀，至少包含 routeId）
     */
    void deleteAll(boolean persistNow, Object... primaryKeys);


}
