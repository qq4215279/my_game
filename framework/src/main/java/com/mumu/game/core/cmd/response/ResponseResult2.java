/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.response;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.message.system.message.GameMessageHeader;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * ResponseResult
 * 结果返回消息体
 * @author liuzhen
 * @version 1.0.0 2024/11/28 17:59
 */
@Getter
@Deprecated
public class ResponseResult2 {
    /** 玩家id */
    private long playerId;
    /** 错误码 */
    private ErrorCode errorCode = ErrorCode.SUCCESS;
    /** res */
    private Object resMsg = null;
    
    /** cmd */
    @Setter
    private Cmd cmd;
    /** 请求header */
    @Deprecated
    @Setter
    private GameMessageHeader header = null;

    public boolean isSuccess() {
        return errorCode == ErrorCode.SUCCESS;
    }

    /**
     * 创建简单操作成功返回
     * @param playerId playerId
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:23
     */
    public static ResponseResult2 success(long playerId) {
        return of(playerId, ErrorCode.SUCCESS, null);
    }

    /**
     * 创建成功返回
     * @param playerId playerId
     * @param resMsg   resMsg
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:23
     */
    public static <T> ResponseResult2 success(long playerId, T resMsg) {
        return of(playerId, ErrorCode.SUCCESS, resMsg);
    }

    /**
     * 创建自定义异常
     * @param playerId  playerId
     * @param errorCode errorCode
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:23
     */
    public static ResponseResult2 error(long playerId, ErrorCode errorCode) {
        return of(playerId, errorCode, null);
    }

    /**
     * 创建功能未开放异常返回
     * @param playerId playerId
     * @return com.game.framework.core.cmd.response.ResponseResult
     * @since 2024/12/2 17:43
     */
    public static ResponseResult2 errorByNotOpen(long playerId) {
        return error(playerId, ErrorCode.FAIL_FUNCTION_NOT_OPOEN);
    }

    /**
     * 创建空参数异常返回
     * @param playerId playerId
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:23
     */
    public static ResponseResult2 errorByEmpty(long playerId) {
        return error(playerId, ErrorCode.FAIL_PARAM_EMPTY);
    }

    /**
     * 创建空参数异常返回
     * @param playerId playerId
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:24
     */
    public static ResponseResult2 errorByParam(long playerId) {
        return error(playerId, ErrorCode.FAIL_PARAM_ERROR);
    }

    /**
     * 创建重新登陆异常返回
     * @param playerId playerId
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:24
     */
    public static ResponseResult2 errorByFail(long playerId) {
        return error(playerId, ErrorCode.FAIL);
    }

    /**
     * 创建重新登陆异常返回
     * @param playerId  playerId
     * @param errorCode errorCode
     * @param resMsg    resMsg
     * @return com.game.framework.core.cmd.response.ResponseBody
     * @since 2024/11/28 18:24
     */
    public static ResponseResult2 of(long playerId, ErrorCode errorCode, Object resMsg) {
        ResponseResult2 responseResult = new ResponseResult2();
        responseResult.playerId = playerId;
        responseResult.errorCode = errorCode;
        responseResult.resMsg = resMsg;
        return responseResult;
    }
}
