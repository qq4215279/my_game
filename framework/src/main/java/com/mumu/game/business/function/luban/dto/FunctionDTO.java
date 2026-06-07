package com.mumu.game.business.function.luban.dto;

import com.mumu.game.core.condition.ConditionParser;
import com.mumu.game.luban.config.function.Function;
import com.mumu.game.proto.function.ConfigFunctionInfoBean;

import lombok.Getter;

/**
 * FunctionDTO
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:37
 */
@Getter
public class FunctionDTO {
    /** 功能id */
    private final int functionId;
    /** 父功能id */
    private final int parentId;
    /** 名称 */
    private final String name;
    /** 描述 */
    private final String desc;
    /** 是否关闭 */
    private final boolean close;
    /** 排序 */
    private final int sort;
    /** 功能额外信息 */
    private final String extraInfo;
    /** 条件解析器 */
    private final String condition;

    public FunctionDTO(Function conf) {
        this.functionId = conf.id;
        this.parentId = conf.parentId;
        this.name = conf.name;
        this.desc = conf.desc;
        this.close = conf.close;
        this.sort = conf.sort;
        this.extraInfo = conf.extraInfo;
        this.condition = conf.condition;
    }

    public ConfigFunctionInfoBean buildConfigFunctionInfoBean() {
        ConfigFunctionInfoBean bean = new ConfigFunctionInfoBean();
        bean.setParentId(this.parentId);
        bean.setName(this.name);
        bean.setDesc(this.desc);
        bean.setClose(this.close);
        bean.setSort(this.sort);
        bean.setExtraInfo(this.extraInfo);
        return bean;
    }

    // TODO
    public String[] getActivityType() {
        return null;
    }
}
