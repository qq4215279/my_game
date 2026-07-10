package com.mumu.game.core.db.bootstrap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.config.DbPersistProperties;
import com.mumu.game.core.db.core.BaseEntity;
import com.mumu.game.core.db.core.BaseModel;
import com.mumu.game.core.db.meta.ModelMeta;
import com.mumu.game.core.db.meta.ModelRegistry;
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

    @Resource
    private DbPersistProperties persistProperties;

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }


    @Override
    public void autoInit() {
        Map<String, BaseModel> modelBeans = SpringContextUtils.getBeansOfType(BaseModel.class);
        for (BaseModel<?> model : modelBeans.values()) {
            Class<? extends BaseEntity> domainClass = resolveDomainClass(model.getClass());
            ModelTable table = domainClass.getAnnotation(ModelTable.class);
            if (table == null) {
                throw new IllegalStateException("Model 对应实体缺少 @ModelTable: " + domainClass.getName());
            }
            String engine = persistProperties.resolveEngine(table.name());
            ModelMeta meta = ModelMeta.parse(domainClass, table, engine);
            ModelRegistry.registerDomain(meta);
            model.bindMeta(meta);
            ModelRegistry.registerModel(domainClass, model);
            LogTopic.MODEL.info("registerModel", "table", meta.getTableName(), "domain", domainClass.getSimpleName(),
                "strategy", meta.getPersistStrategy(), "engine", engine);
        }
    }

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
}
