package com.mumu.game.template.func.core.temp;

import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.proto.function.SingleFunctionInfoBean;
import com.mumu.game.template.func.domain.PlayerTemplateState;

import java.util.List;

/**
 * Template
 * 功能模版
 * @author liuzhen
 * @version 1.0.0 2026/6/7 14:39
 */
public interface Template {
    /**
     * 获取功能id
     * @return int
     * @since 2025/7/4 18:06
     */
    int getFunctionId();

    /**
     * 是否开启
     * @param playerId playerId
     * @return boolean
     * @since 2025/7/4 18:06
     */
    boolean isOpen(long playerId);

    /**
     * 获取当前状态
     * @param playerId playerId
     * @return com.game.template.func.domain.PlayerTemplateState
     * @since 2025/7/4 18:06
     */
    PlayerTemplateState getState(long playerId);

    /**
     * 获取当前模版下，所有子功能state(注：包括孙)
     * @param playerId playerId
     * @return java.util.List<com.game.template.func.domain.PlayerTemplateState>
     * @since 2025/7/9 16:36
     */
    List<PlayerTemplateState> getStateList(long playerId);

    /**
     * 清除功能红点
     * @param playerId playerId
     * @param arg0 参数0
     * @param arg1 参数1
     * @return com.game.framework.core.cmd.response.ResponseResult
     * @since 2025/7/4 18:06
     */
    ResponseResult clearFuncRedPoint(long playerId, int arg0, String arg1);

    /**
     * 推送功能红点状态消息
     * @param playerId playerId
     * @since 2025/7/4 18:07
     */
    void pushFunctionStateMessage(long playerId);

    /**
     * 构建功能信息bean列表
     * @param playerId playerId
     * @param buildAll buildConfigInfo
     * @return java.util.List<com.game.proto.function.SingleFunctionInfoBean>
     * @since 2025/7/4 18:07
     */
    List<SingleFunctionInfoBean> buildSingleFunctionInfoBeanList(long playerId, boolean buildAll);

}
