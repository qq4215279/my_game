package com.mumu.game.template.func.core.temp;

import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * DefaultTemplate
 * 默认功能模版
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:31
 */
@Component
@Scope("prototype")
public class DefaultTemplate extends AbstractTemplate {

    /** 功能id */
    @Setter
    protected int functionId;

    @Override
    public int getFunctionId() {
        return this.functionId;
    }

}
