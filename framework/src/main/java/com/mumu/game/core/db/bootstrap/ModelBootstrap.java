package com.mumu.game.core.db.bootstrap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.util.RandomUtil;
import com.mumu.game.core.db.consts.ModelConstants;
import com.mumu.game.core.db.core.dirty.DirtyEntry;
import com.mumu.game.core.db.core.dirty.DirtyTracker;
import com.mumu.game.core.db.lifecycle.PersistThreadPool;
import com.mumu.game.core.thread.ScheduledExecutorUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.config.DbPersistProperties;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.SpringContextUtils;

/**
 * ModelBootstrap
 * 模型启动注册与校验
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelBootstrap implements AutoInitEvent {
    /** 每张表当前处理分片 */
    private final Map<String, AtomicInteger> portionMap = new ConcurrentHashMap<>();

    @Resource
    private DbPersistProperties persistProperties;
    @Resource
    private DirtyTracker dirtyTracker;
    @Resource
    private PersistThreadPool persistThreadPool;

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }


    @Override
    public void autoInit() {
        // 初始化模型表
        initModelTable();

        // 初始化模型表任务
        initModelPersistSchedule();
    }

    /**
     * 初始化模型表
     * @since 2026/7/14 14:09
     */
    private void initModelTable() {
        Map<String, BaseModel> modelBeans = SpringContextUtils.getBeansOfType(BaseModel.class);
        for (BaseModel<?> model : modelBeans.values()) {
            Class<? extends BaseEntity> domainClass = resolveDomainClass(model.getClass());
            ModelTable table = domainClass.getAnnotation(ModelTable.class);
            if (table == null) {
                throw new IllegalStateException("Model 对应实体缺少 @ModelTable: " + domainClass.getName());
            }
            String engine = persistProperties.resolveEngine(table.name());
            ModelMeta meta = ModelMeta.parse(domainClass, table, engine);
            ModelRegistry.registerEntity(meta);
            model.bindMeta(meta);
            ModelRegistry.registerModel(domainClass, model);
            LogTopic.MODEL.info("registerModel", "table", meta.getTableName(), "domain", domainClass.getSimpleName(),
                "strategy", meta.getPersistStrategy(), "engine", engine);
        }
    }

    /** 解析实体类 */
    @SuppressWarnings("unchecked")
    private Class<? extends BaseEntity> resolveDomainClass(Class<?> modelClass) {
        Type type = modelClass.getGenericSuperclass();
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> domainClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            if (!BaseEntity.class.isAssignableFrom(domainClass)) {
                throw new IllegalStateException("Model 泛型必须继承 AbstractDomain: " + modelClass.getName());
            }
            return (Class<? extends BaseEntity>) domainClass;
        }
        throw new IllegalStateException("无法解析 Model 泛型: " + modelClass.getName());
    }


    /**
     * 初始化模型定时持久化与失败重试任务（分片批量处理）
     */
    private void initModelPersistSchedule() {
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.hasDb() && !meta.hasRedis()) {
                continue;
            }
            portionMap.put(meta.getTableName(), new AtomicInteger(0));
            String scheduleKey = "persist:" + meta.getTableName();
            long initialDelay = RandomUtil.randomInt(10000, 30000);
            // 定义任务
            ScheduledExecutorUtil.scheduleWithFixedDelay(scheduleKey, () -> flushTable(meta), initialDelay,
                    meta.getPersistInterval(), TimeUnit.MILLISECONDS);
            LogTopic.MODEL.info("registerPersistSchedule", "table", meta.getTableName(), "interval",
                    meta.getPersistInterval(), "initialDelay", initialDelay);
        }
    }

    private void flushTable(ModelMeta meta) {
        AtomicInteger portionCounter = portionMap.get(meta.getTableName());
        if (portionCounter == null) {
            return;
        }
        int portion = Math.floorMod(portionCounter.getAndIncrement(), ModelConstants.EVENT_SYN_RATIO);
        List<DirtyEntry> entries = dirtyTracker.listByTableAndPortion(meta.getTableName(), portion, ModelConstants.EVENT_SYN_RATIO);
        if (entries.isEmpty()) {
            return;
        }
        BaseModel<?> model = ModelRegistry.getModelBean(meta.getEntityClass());
        int count = 0;
        for (DirtyEntry entry : entries) {
            persistThreadPool.submit(entry.getRouteId(), meta.getTableName(), entry.getCacheKey(),
                    () -> model.retryFlush(entry.getCacheKey()));
            if (++count >= ModelConstants.PERSIST_BATCH_SIZE) {
                break;
            }
        }
    }
}
