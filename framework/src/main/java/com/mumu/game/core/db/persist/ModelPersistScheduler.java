package com.mumu.game.core.db.persist;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.db.consts.ModelConstants;
import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.dirty.DirtyEntry;
import com.mumu.game.core.db.dirty.DirtyTracker;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.meta.ModelRegistry;
import com.mumu.game.core.db.pool.PersistThreadPool;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.thread.ScheduledExecutorUtil;

import cn.hutool.core.util.RandomUtil;

/**
 * ModelPersistScheduler
 * 模型定时持久化与失败重试（分片批量处理）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class ModelPersistScheduler implements AutoInitEvent {

    /** 每张表当前处理分片 */
    private final Map<String, AtomicInteger> portionMap = new ConcurrentHashMap<>();

    @Autowired
    private DirtyTracker dirtyTracker;
    @Autowired
    private PersistThreadPool persistThreadPool;

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void autoInit() {
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
