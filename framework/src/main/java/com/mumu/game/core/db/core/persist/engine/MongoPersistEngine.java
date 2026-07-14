package com.mumu.game.core.db.core.persist.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

import com.mumu.game.core.db.bootstrap.ModelRegistry;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.meta.IndexMeta;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.core.persist.PersistEngine;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.mongo.config.MongoDB;
import com.mumu.game.expcetion.ModelPersistException;

/**
 * MongoPersistEngine
 * MongoDB 持久化引擎（L3），使用 {@link MongoDB#MODEL} 库模版
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
    @SuppressWarnings("unchecked")
    public <Entity extends BaseEntity> Entity findOne(ModelMeta meta, IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        try {
            MongoTemplate template = getTemplate();
            Class<Entity> clazz = (Class<Entity>) index.getEntityClass();
            Entity entity = template.findOne(buildQuery(index, keys), clazz, meta.getTableName());
            if (entity != null) {
                entity.marshal();
            }
            return entity;
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoFindOne", "table", meta.getTableName(), "index", index.getName());
            throw new ModelPersistException("MongoDB 查询失败: " + meta.getTableName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Domain extends BaseEntity> List<Domain> findList(ModelMeta meta, IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0) {
            return Collections.emptyList();
        }
        try {
            MongoTemplate template = getTemplate();
            Class<Domain> clazz = (Class<Domain>) index.getEntityClass();
            List<Domain> list = template.find(buildQuery(index, keys), clazz, meta.getTableName());
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            for (Domain entity : list) {
                entity.marshal();
            }
            return list;
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoFindList", "table", meta.getTableName(), "index", index.getName());
            throw new ModelPersistException("MongoDB 列表查询失败: " + meta.getTableName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void upsert(ModelMeta meta, BaseEntity entity) {
        try {
            entity.unmarshal();
            MongoTemplate template = getTemplate();
            IndexMeta primary = meta.getPrimaryIndex();
            Object[] keys = primary.readKeyValues(entity);
            Query query = buildQuery(primary, keys);
            Class<BaseEntity> clazz = (Class<BaseEntity>) meta.getEntityClass();
            template.findAndReplace(query, entity, FindAndReplaceOptions.options().upsert(), clazz, meta.getTableName());
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoUpsert", "table", meta.getTableName());
            throw new ModelPersistException("MongoDB 写入失败: " + meta.getTableName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void upsertBatch(ModelMeta meta, Collection<? extends BaseEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        try {
            MongoTemplate template = getTemplate();
            IndexMeta primary = meta.getPrimaryIndex();
            Class<BaseEntity> clazz = (Class<BaseEntity>) meta.getEntityClass();
            FindAndReplaceOptions options = FindAndReplaceOptions.options().upsert();
            BulkOperations bulkOps = template.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz, meta.getTableName());
            for (BaseEntity entity : entities) {
                entity.unmarshal();
                Object[] keys = primary.readKeyValues(entity);
                bulkOps.replaceOne(buildQuery(primary, keys), entity, options);
            }
            bulkOps.execute();
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoUpsertBatch", "table", meta.getTableName(), "size", entities.size());
            throw new ModelPersistException("MongoDB 批量写入失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void delete(ModelMeta meta, IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0 || !index.isFullKey(keys)) {
            return;
        }
        try {
            MongoTemplate template = getTemplate();
            template.remove(buildQuery(index, keys), meta.getTableName());
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoDelete", "table", meta.getTableName(), "index", index.getName());
            throw new ModelPersistException("MongoDB 删除失败: " + meta.getTableName(), e);
        }
    }

    @Override
    public void deleteByPrefix(ModelMeta meta, IndexMeta index, Object... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            MongoTemplate template = getTemplate();
            template.remove(buildQuery(index, keys), meta.getTableName());
        } catch (ModelPersistException e) {
            throw e;
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "mongoDeleteByPrefix", "table", meta.getTableName(), "index", index.getName());
            throw new ModelPersistException("MongoDB 批量删除失败: " + meta.getTableName(), e);
        }
    }

    /**
     * 按索引字段左前缀构建等值 Query（完整键 / 前缀键均可）
     */
    private Query buildQuery(IndexMeta index, Object... keys) {
        String[] fields = index.getFields();
        int n = Math.min(keys.length, fields.length);
        List<Criteria> criteriaList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            criteriaList.add(Criteria.where(fields[i]).is(keys[i]));
        }
        if (criteriaList.isEmpty()) {
            return new Query();
        }
        if (criteriaList.size() == 1) {
            return new Query(criteriaList.getFirst());
        }
        return new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }


    /** 获取模版 */
    private MongoTemplate getTemplate() {
        MongoTemplate template = MongoDB.MODEL.template();
        if (template == null) {
            throw new ModelPersistException("MongoTemplate 未初始化: MongoDB.MODEL");
        }
        return template;
    }

    /**
     * 启动期：为所有落 Mongo 的表按主索引字段创建 unique index（幂等）
     */
    public void ensurePrimaryUniqueIndexes() {
        MongoTemplate template = getTemplate();
        for (ModelMeta meta : ModelRegistry.allMeta()) {
            if (!meta.hasDb() || !isMongoEngine(meta)) {
                continue;
            }
            IndexMeta primary = meta.getPrimaryIndex();
            String[] fields = primary.getFields();
            if (fields == null || fields.length == 0) {
                continue;
            }
            try {
                Index index = new Index();
                for (String field : fields) {
                    index.on(field, Sort.Direction.ASC);
                }
                String indexName = "uk_" + primary.getName();
                index.unique().named(indexName);
                template.indexOps(meta.getTableName()).ensureIndex(index);
                LogTopic.MODEL.info("mongoUniqueIndexOk", "table", meta.getTableName(),
                        "index", indexName, "fields", String.join(",", fields));
            } catch (Exception e) {
                LogTopic.MODEL.error(e, "mongoUniqueIndexFail", "table", meta.getTableName(),
                        "index", primary.getName());
                throw new ModelPersistException(
                        "MongoDB 主索引 unique 创建失败（请检查是否已有重复业务键）: " + meta.getTableName(), e);
            }
        }
    }

    /** 是否是Mongo殷勤 */
    private boolean isMongoEngine(ModelMeta meta) {
        String engine = meta.getPersistEngine();
        return StringUtils.isBlank(engine) || ENGINE_TYPE.equalsIgnoreCase(engine);
    }
}
