package com.mumu.game.core.db.core;

import java.util.List;
import java.util.function.Supplier;

/**
 * Model
 * 数据模型对外 API，定义缓存实体的查询与写操作语义。
 * <p>
 * 读路径（select 系列）：同步执行，依次访问 L1 JVM 缓存 → L2 Redis → L3 DB（持久引擎）。
 * L1/L2 未命中且表配置了 DB 时，首次查询穿透到持久层，命中后回填 L2 + L1，后续不再查 DB。
 * 玩家登录时 {@code preLoad} 表会提前预加载整分片数据，未预加载的数据依赖 select 懒加载穿透 DB。
 * <p>
 * 写路径（insert / update / delete）：默认异步落库（标记 dirty 后由持久化线程 flush）；
 * {@code persistNow = true} 时同步 flush 到 Redis / DB。
 * <p>
 * 写操作约束：
 * <ul>
 *   <li>update / delete(entity) 要求传入对象与 JVM 缓存中为同一引用</li>
 *   <li>写操作需在对应 routeId 的业务线程执行（全局表可通过 {@code skipThreadCheck} 跳过）</li>
 * </ul>
 *
 * @param <Entity> 实体类型
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public interface Model<Entity extends BaseEntity> {

    /**
     * 按主索引完整键查询单条记录。
     * <p>读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。</p>
     *
     * @param primaryKeys 主索引完整键（键数量须与主索引字段数一致）
     * @return 实体对象，不存在返回 {@code null}
     * @throws IllegalArgumentException 索引键不完整时抛出
     */
    Entity selectOne(Object... primaryKeys);

    /**
     * 按指定索引完整键查询单条记录。
     * <p>读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。</p>
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引完整键（键数量须与该索引字段数一致）
     * @return 实体对象，不存在返回 {@code null}
     * @throws IllegalArgumentException 索引键不完整时抛出
     */
    Entity selectOne(String indexName, Object... primaryKeys);

    /**
     * 按主索引键查询列表。
     * <p>
     * 完整键时等价于 {@code selectOne} 的单条结果；
     * 左前缀键（键数量小于索引字段数）时返回该前缀下所有匹配记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * </p>
     *
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 匹配列表，无数据返回空列表
     */
    List<Entity> selectList(Object... primaryKeys);

    /**
     * 按指定索引键查询列表。
     * <p>
     * 完整键时等价于 {@code selectOne} 的单条结果；
     * 左前缀键时返回该前缀下所有匹配记录。
     * 读路径：L1 JVM → L2 Redis → L3 DB；命中时回填上层缓存。
     * </p>
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 匹配列表，无数据返回空列表
     */
    List<Entity> selectList(String indexName, Object... primaryKeys);

    /**
     * 按主索引键查询列表，结果按 cacheKey 倒序排列。
     * <p>语义同 {@link #selectList(Object...)}，仅排序方式不同。</p>
     *
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 倒序匹配列表，无数据返回空列表
     */
    List<Entity> selectListReverse(Object... primaryKeys);

    /**
     * 按指定索引键查询列表，结果按 cacheKey 倒序排列。
     * <p>语义同 {@link #selectList(String, Object...)}，仅排序方式不同。</p>
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引键（完整键或左前缀）
     * @return 倒序匹配列表，无数据返回空列表
     */
    List<Entity> selectListReverse(String indexName, Object... primaryKeys);

    /**
     * 查询单条记录，不存在则创建并插入。
     * <p>默认异步持久化，等价于 {@code selectOrCreate(builder, false, primaryKeys)}。</p>
     *
     * @param builder     记录不存在时的实体构造器
     * @param primaryKeys 主索引完整键
     * @return 已存在或新创建的实体
     */
    default Entity selectOrCreate(Supplier<Entity> builder, Object... primaryKeys) {
        return selectOrCreate(builder, false, primaryKeys);
    }

    /**
     * 查询单条记录，不存在则创建并插入。
     *
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
     *
     * @param builder     记录不存在时的实体构造器
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引完整键
     * @return 已存在或新创建的实体
     */
    default Entity selectOrCreate(Supplier<Entity> builder, boolean persistNow, String indexName, Object... primaryKeys) {
        Entity entity = selectOne(indexName, primaryKeys);
        if (entity == null) {
            entity = builder.get();
            insert(entity, persistNow);
        }
        return entity;
    }

    /**
     * 更新实体（异步持久化）。
     * <p>
     * 实体须已存在于 JVM 缓存，且传入对象与缓存中为同一引用。
     * 多次 update 同一记录仅标记一次 dirty，flush 时读取 JVM 最新对象。
     * </p>
     *
     * @param entity 待更新实体（须为 JVM 缓存中的同一引用）
     * @throws IllegalStateException 记录不存在或引用不一致时抛出
     */
    void update(Entity entity);

    /**
     * 更新实体。
     *
     * @param entity     待更新实体（须为 JVM 缓存中的同一引用）
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @throws IllegalStateException 记录不存在或引用不一致时抛出
     */
    void update(Entity entity, boolean persistNow);

    /**
     * 插入新记录（异步持久化）。
     * <p>记录已存在时抛出异常；写入 JVM 缓存并标记 dirty。</p>
     *
     * @param entity 待插入实体
     * @throws IllegalStateException 记录已存在时抛出
     */
    void insert(Entity entity);

    /**
     * 插入新记录。
     *
     * @param entity     待插入实体
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @throws IllegalStateException 记录已存在时抛出
     */
    void insert(Entity entity, boolean persistNow);

    /**
     * 插入或更新（异步持久化）。
     * <p>
     * 记录不存在时执行 insert；已存在时执行 update。
     * 存在性判断路径：L1 JVM → L2 Redis → L3 DB，命中后回填上层缓存。
     * update 分支要求传入对象与 JVM 缓存中为同一引用。
     * </p>
     *
     * @param entity 待写入实体
     * @throws IllegalStateException update 时引用不一致时抛出
     */
    void insertOrUpdate(Entity entity);

    /**
     * 按实体对象删除（异步持久化）。
     * <p>
     * 从 JVM 缓存移除记录并标记 DELETE dirty。
     * 若缓存中仍有该记录，传入对象须与缓存中为同一引用。
     * </p>
     *
     * @param entity 待删除实体
     * @throws IllegalStateException 引用不一致时抛出
     */
    void delete(Entity entity);

    /**
     * 按实体对象删除。
     *
     * @param entity     待删除实体
     * @param persistNow 是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @throws IllegalStateException 引用不一致时抛出
     */
    void delete(Entity entity, boolean persistNow);

    /**
     * 按主索引完整键删除单条记录（异步持久化）。
     * <p>无需传入实体对象，直接从 JVM 缓存移除并标记 DELETE dirty。</p>
     *
     * @param primaryKeys 主索引完整键
     * @throws IllegalArgumentException 索引键不完整时抛出
     */
    void deleteOne(Object... primaryKeys);

    /**
     * 按主索引完整键删除单条记录。
     *
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param primaryKeys 主索引完整键
     * @throws IllegalArgumentException 索引键不完整时抛出
     */
    void deleteOne(boolean persistNow, Object... primaryKeys);

    /**
     * 按指定索引完整键删除单条记录。
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param primaryKeys 索引完整键
     * @throws IllegalArgumentException 索引键不完整时抛出
     */
    void deleteOne(String indexName, boolean persistNow, Object... primaryKeys);

    /**
     * 按主索引左前缀批量删除（异步持久化）。
     * <p>删除 JVM 缓存中该前缀下所有匹配记录，并逐条标记 DELETE dirty。</p>
     *
     * @param primaryKeys 索引键（左前缀，至少包含 routeId）
     */
    void deleteAll(Object... primaryKeys);

    /**
     * 按指定索引左前缀批量删除（异步持久化）。
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param primaryKeys 索引键（左前缀，至少包含 routeId）
     */
    void deleteAll(String indexName, Object... primaryKeys);

    /**
     * 按指定索引左前缀批量删除。
     *
     * @param indexName   索引名称（对应 {@code @Index.name}）
     * @param persistNow  是否同步持久化（{@code true} 立即 flush 到 Redis / DB）
     * @param primaryKeys 索引键（左前缀，至少包含 routeId）
     */
    void deleteAll(String indexName, boolean persistNow, Object... primaryKeys);

}
