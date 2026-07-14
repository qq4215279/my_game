package com.mumu.game.core.db.bootstrap;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.core.meta.ModelMeta;

/**
 * ModelRegistry
 * 模型注册中心，保存启动期解析的表元数据与 Model Bean 映射。
 * <p>由 {@link com.mumu.game.core.db.bootstrap.ModelBootstrap} 在 CORE 阶段填充</p>
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
public final class ModelRegistry {

    /** 实体类 -> 表元数据 */
    private static final Map<Class<?>, ModelMeta> DOMAIN_META = new ConcurrentHashMap<>();
    /** 实体类 -> Model 实例 */
    private static final Map<Class<?>, BaseModel<?>> DOMAIN_MODEL = new ConcurrentHashMap<>();
    /** 表名 -> 表元数据 */
    private static final Map<String, ModelMeta> TABLE_META = new ConcurrentHashMap<>();

    private ModelRegistry() {
    }

    /**
     * 注册实体元数据
     */
    public static void registerEntity(ModelMeta meta) {
        DOMAIN_META.put(meta.getEntityClass(), meta);
        TABLE_META.put(meta.getTableName(), meta);
    }

    /**
     * 注册 Model Bean
     */
    public static void registerModel(Class<? extends BaseEntity> domainClass, BaseModel<?> model) {
        DOMAIN_MODEL.put(domainClass, model);
    }

    /**
     * 按实体类获取元数据
     */
    public static ModelMeta getMeta(Class<?> domainClass) {
        ModelMeta meta = DOMAIN_META.get(domainClass);
        if (meta == null) {
            throw new IllegalStateException("未注册实体元数据: " + domainClass.getName());
        }
        return meta;
    }

    /**
     * 按表名获取元数据
     */
    public static ModelMeta getMetaByTable(String tableName) {
        ModelMeta meta = TABLE_META.get(tableName);
        if (meta == null) {
            throw new IllegalStateException("未注册表元数据: " + tableName);
        }
        return meta;
    }

    /**
     * 按实体类获取 Model（带泛型）
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseEntity> BaseModel<T> getModel(Class<T> domainClass) {
        return (BaseModel<T>) getModelBean(domainClass);
    }

    /**
     * 按实体类获取 Model
     */
    public static BaseModel<?> getModelBean(Class<?> domainClass) {
        BaseModel<?> model = DOMAIN_MODEL.get(domainClass);
        if (model == null) {
            throw new IllegalStateException("未注册 Model: " + domainClass.getName());
        }
        return model;
    }

    /**
     * 获取全部已注册表元数据
     */
    public static Collection<ModelMeta> allMeta() {
        return DOMAIN_META.values();
    }
}
