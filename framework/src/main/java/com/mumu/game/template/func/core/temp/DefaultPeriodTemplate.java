package com.mumu.game.template.func.core.temp;

import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * DefaultPeriodTemplate
 * 默认的周期性功能模板
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:31
 */
@Component
@Scope("prototype")
public class DefaultPeriodTemplate extends AbstractPeriodTemplate {

    /** 功能id */
    @Setter
    protected int functionId;

    @Override
    public int getFunctionId() {
        return functionId;
    }
}