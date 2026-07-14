package com.mumu.game.core.mongo.config;

import com.mongodb.client.MongoClients;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MongoConfig
 * MongoDB多数据库配置
 * @author liuzhen
 * @version 1.0.0 2026/7/14 16:14
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class MongoConfig {
    @Resource
    private MongoProperties mongoProperties;


    @Bean
    public MongoTemplate mongoTemplate() {
        // TODO Mongo 接入 暂时只用于test环境！
        MongoDB[] values = MongoDB.values();
        if (values.length == 0) {
            return null;
        }

        Map<String, MongoTemplate> mongoTemplateMap = new HashMap<>();
        for (MongoDB mongo : values) {
            String database = mongo.getDatabase();
            mongoTemplateMap.put(database,
                    new MongoTemplate(MongoClients.create(getUri(database)), database));
        }
        MongoDB.mongoTemplateMap = mongoTemplateMap;
        return MongoDB.MODEL.template();
    }

    /**
     * 获取MongoDB链接uri
     * @param database database
     * @return java.lang.String
     * @since 2025/7/21 11:31
     */
    private String getUri(String database) {
        // mongodb://139.224.56.39:27017/
        return "mongodb://" + mongoProperties.getHost() + ":" + mongoProperties.getPort() + "/" + database;
    }
}
