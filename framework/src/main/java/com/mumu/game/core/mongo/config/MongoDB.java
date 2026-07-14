package com.mumu.game.core.mongo.config;

import lombok.Getter;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * MongoDB
 * MongoDB库
 * @author liuzhen
 * @version 1.0.0 2026/7/14 16:14
 */
@Getter
public enum MongoDB {
    /** 游戏库 */
    MODEL("model"),
    ;

    /** 数据库名称 */
    private final String database;

    MongoDB(String database) {
        this.database = database;
    }

    /** 库名 与 模版映射 */
    static Map<String, MongoTemplate> mongoTemplateMap = Collections.emptyMap();

    /**
     * 获取模版
     * @return org.springframework.data.mongodb.core.MongoTemplate
     * @since 2026/7/14 16:15
     */
    public MongoTemplate template() {
        return mongoTemplateMap.get(database);
    }

    /**
     * 获取MongoDB数据库操作模版
     * @param database database
     * @return org.springframework.data.mongodb.core.MongoTemplate
     * @since 2026/7/14 16:15
     */
    public static MongoTemplate getMongoTemplate(String database) {
        return mongoTemplateMap.get(database);
    }
}
