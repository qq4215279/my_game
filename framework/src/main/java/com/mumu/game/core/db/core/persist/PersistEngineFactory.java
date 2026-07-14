package com.mumu.game.core.db.core.persist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.mumu.game.core.db.core.persist.engine.MongoPersistEngine;
import com.mumu.game.core.db.core.meta.ModelMeta;

/**
 * PersistEngineFactory
 * 持久化引擎工厂（未配置时默认 MongoDB）
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/9
 */
@Component
public class PersistEngineFactory {

    private final Map<String, PersistEngine> engineMap = new HashMap<>();
    private final PersistEngine defaultEngine;

    public PersistEngineFactory(List<PersistEngine> engines, MongoPersistEngine mongoPersistEngine) {
        for (PersistEngine engine : engines) {
            engineMap.put(engine.type(), engine);
        }
        this.defaultEngine = mongoPersistEngine;
    }

    public PersistEngine getEngine(ModelMeta meta) {
        String engineType = meta.getPersistEngine();
        if (StringUtils.isBlank(engineType)) {
            return defaultEngine;
        }
        PersistEngine engine = engineMap.get(engineType);
        if (engine == null) {
            return defaultEngine;
        }
        return engine;
    }
}
