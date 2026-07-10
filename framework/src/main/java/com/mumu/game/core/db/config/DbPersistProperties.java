package com.mumu.game.core.db.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.engine.mongo.MongoPersistEngine;

import lombok.Data;

/**
 * DbPersistProperties
 * 数据持久化配置
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Data
@Component
@ConfigurationProperties(prefix = "db.persist")
public class DbPersistProperties {

    /** 默认引擎（未配置表级引擎时使用） */
    private String defaultEngine = MongoPersistEngine.ENGINE_TYPE;

    /** 表级引擎配置 tableName -> engineType */
    private Map<String, String> engines = new HashMap<>();

    /** 是否开启写操作线程校验 */
    private boolean threadCheckEnabled = true;

    /** 线程校验失败时是否抛异常（false 仅打日志） */
    private boolean strictThreadCheck = false;

    public String resolveEngine(String tableName) {
        String engine = engines.get(tableName);
        if (StringUtils.isNotBlank(engine)) {
            return engine;
        }
        return defaultEngine;
    }
}
