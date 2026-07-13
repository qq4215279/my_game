package com.mumu.game.core.db.engine.mongo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.engine.PersistEngine;
import com.mumu.game.core.db.meta.IndexMeta;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.util.EntitySerializer;
import com.mumu.game.core.log.LogTopic;

/**
 * MongoPersistEngine
 * MongoDB 持久化引擎（默认实现，待接入 MongoTemplate）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class MongoPersistEngine implements PersistEngine {

    public static final String ENGINE_TYPE = "mongo";

    @Override
    public String type() {
        return ENGINE_TYPE;
    }

    @Override
    public <Domain extends BaseEntity> Domain findOne(ModelMeta meta, IndexMeta index, Class<Domain> clazz, Object... keys) {
        LogTopic.MODEL.info("mongoFindOneStub", "table", meta.getTableName(), "index", index.getName(), "keys", keys);
        return null;
    }

    @Override
    public <Domain extends BaseEntity> List<Domain> findList(ModelMeta meta, IndexMeta index, Class<Domain> clazz,
                                                             Object... keys) {
        LogTopic.MODEL.info("mongoFindListStub", "table", meta.getTableName(), "index", index.getName(), "keys", keys);
        return Collections.emptyList();
    }

    @Override
    public void upsert(ModelMeta meta, BaseEntity entity) {
        try {
            entity.unmarshal();
            String json = EntitySerializer.serialize(meta, entity);
            LogTopic.MODEL.info("mongoUpsertStub", "table", meta.getTableName(), "data", json);
            // TODO 接入 MongoTemplate 后实现真实 upsert
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoUpsert", "table", meta.getTableName());
            throw new IllegalStateException("MongoDB 写入失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void upsertBatch(ModelMeta meta, Collection<? extends BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            upsert(meta, entity);
        }
    }

    @Override
    public void delete(ModelMeta meta, IndexMeta index, Object... keys) {
        LogTopic.MODEL.info("mongoDeleteStub", "table", meta.getTableName(), "index", index.getName(), "keys", keys);
        // TODO 接入 MongoTemplate 后实现真实 delete
    }

    @Override
    public void deleteByPrefix(ModelMeta meta, IndexMeta index, Object... keys) {
        LogTopic.MODEL.info("mongoDeleteByPrefixStub", "table", meta.getTableName(), "index", index.getName(), "keys", keys);
        // TODO 接入 MongoTemplate 后实现真实前缀删除
    }
}
