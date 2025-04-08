/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.cmd.param.parse.ParamParse;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.context.GameMessageContextImpl;
import com.mumu.framework.util.JProtoBufUtil;

import lombok.Data;

/**
 * ActionInvocation
 * action映射器
 * @author liuzhen
 * @version 1.0.0 2024/12/5 22:27
 */
@Data
public class ActionInvocation {
    /** CMD */
    private int messageId;
    /** 标记了 @CmdAction 的 action 对象 */
    private Object action;
    /** 标记了 @CmdMapping 的 action 对象中的方法 */
    private Method method;
    /** 提高反射方法调用性能 */
    private MethodAccess methodAccess;

    /** action名称  */
    protected String actionName;
    /** method名称  */
    protected String methodName;
    /** TODO 参数解析器 */
    protected ParamParse paramParse;

    public ActionInvocation(int messageId, Object action, Method method) {
        this.actionName = action.getClass().getSimpleName();
        this.methodName = method.getName();

        this.messageId = messageId;
        this.action = action;
        this.method = method;
        this.methodAccess = MethodAccess.get(action.getClass());
    }

    /** TODO 执行目标事件 */
    public void invokeMethod(GameMessageContextImpl gameMessageContext) {
        // TODO
        GameMessagePackage reqGameMessagePackage = gameMessageContext.getReqGameMessagePackage();
        Class<?> reqMsgClass = CmdManager.getCmd(reqGameMessagePackage.getHeader().getMessageId()).getReqMsgClass();

        Object reqMsg = JProtoBufUtil.decode(reqGameMessagePackage.getBody(), reqMsgClass);
        Object res = methodAccess.invoke(action, method.getName(), gameMessageContext.getPlayerId(), reqMsg);
        if (res instanceof ResponseResult responseResult) {
            // TODO GameChannelPromise promise
            responseResult.setCmd(CmdManager.getCmd(messageId));
            responseResult.setHeader(reqGameMessagePackage.getHeader());
            gameMessageContext.getGameContext().writeAndFlush(responseResult);
        }
    }
}
