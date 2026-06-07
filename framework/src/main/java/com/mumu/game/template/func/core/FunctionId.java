package com.mumu.game.template.func.core;

import com.mumu.game.template.func.core.temp.Template;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * FunctionId
 * 功能id枚举
 * @author liuzhen
 * @version 1.0.0 2026/6/7 14:32
 */
public enum FunctionId {
    /** 顶层父功能id */
    ROOT(0),

    /** 背包 */
    BAG(1),


    ;

    /** 功能id */
    @Getter
    private final int functionId;

    /** 功能id 与 枚举 映射 */
    private static final Map<Integer, FunctionId> FUNCTIONID_MAP = new HashMap<>();

    static {
        for (FunctionId functionId : values()) {
            FUNCTIONID_MAP.put(functionId.functionId, functionId);
        }
    }

    /**
     * 获取功能id对应枚举
     * @param functionId functionId
     * @return com.mumu.game.template.func.core.FunctionId
     */
    public static FunctionId getFunctionId(int functionId) {
        return FUNCTIONID_MAP.get(functionId);
    }

    FunctionId(int functionId) {
        this.functionId = functionId;
    }

    /**
     * 获取功能模版
     * @param playerId playerId
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T extends Template> T loadFuncTemplate(long playerId) {
        return (T) TemplateManager.loadFuncTemplate(playerId, functionId);
    }

    /**
     * 获取功能模版
     *
     * @param clazz clazz
     * @return T
     * @since 2024/12/13 17:04
     */
    public <T extends Template> T loadFuncTemplate(long playerId, Class<T> clazz) {
        return clazz.cast(TemplateManager.loadFuncTemplate(playerId, functionId));
    }

    /**
     * 功能开放
     *
     * @return boolean
     * @since 2024/12/13 14:34
     */
    public boolean isOpen(long playerId) {
        return loadFuncTemplate(playerId, functionId).isOpen(playerId);
    }

    /**
     * 触发红点推送
     *
     * @since 2024/12/13 14:33
     */
    public void pushFunctionStateMessage(long playerId) {
        loadFuncTemplate(playerId, functionId).pushFunctionStateMessage(playerId);
    }

    // ===================================== 【static】 =====================================

    /**
     * 加载功能模版
     *
     * @param playerId playerId
     * @param functionId functionId
     * @param loadRemoteState 是否需要预加载远程模版的PlayerTemplateState
     * @return com.game.template.func.core.temp.Template
     * @since 2025/7/9 15:57
     */
    public static Template loadFuncTemplate(long playerId, int functionId, boolean loadRemoteState) {
        return TemplateManager.loadFuncTemplate(playerId, functionId, loadRemoteState);
    }

    /**
     * 加载功能模版(默认无需加载远程模版state列表)
     *
     * @param playerId playerId
     * @param functionId functionId
     * @return com.game.template.func.core.temp.Template
     * @since 2025/7/9 15:57
     */
    public static Template loadFuncTemplate(long playerId, int functionId) {
        return TemplateManager.loadFuncTemplate(playerId, functionId);
    }
}
