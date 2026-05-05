/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.mumu.game.core.cmd.enums.CmdManager;
import com.mumu.game.core.cmd.enums.ICmd;
import com.mumu.game.core.cmd.param.parse.ParamParse;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.utils.JProtoBufUtil;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

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
    private ICmd cmd;
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

    public ActionInvocation(ICmd cmd, Object action, Method method) {
        this.actionName = action.getClass().getSimpleName();
        this.methodName = method.getName();

        this.cmd = cmd;
        this.action = action;
        this.method = method;
        this.methodAccess = MethodAccess.get(action.getClass());
    }

    /** TODO 执行目标事件 */
    /* @Deprecated
    public void invokeMethod(GameMessageContextImpl gameMessageContext) {
        GameMessagePackage reqGameMessagePackage = gameMessageContext.getReqGameMessagePackage();
        Class<?> reqMsgClass = CmdManager.getCmd(reqGameMessagePackage.getHeader().getMessageId()).getReqMsgClass();

        Object reqMsg = JProtoBufUtil.decode(reqGameMessagePackage.getBody(), reqMsgClass);
        Object res = methodAccess.invoke(action, method.getName(), gameMessageContext.getPlayerId(), reqMsg);
        if (res instanceof ResponseResult2 responseResult) {
            responseResult.setCmd(cmd);
            responseResult.setHeader(reqGameMessagePackage.getHeader());
            gameMessageContext.getGameContext().writeAndFlush(responseResult);
        }
    } */

    /**
     * 执行目标事件
     * @param context 消息上下文
     */
    public void invokeMethod(MessageContext context) {
        GameMessagePackage reqGameMessagePackage = context.getMessagePackage();
        GameMessageHeader header = reqGameMessagePackage.getHeader();
        long playerId = header.getPlayerId();
        Class<?> reqMsgClass = CmdManager.getCmd(header.getMessageId()).getReqMsgClass();

        // Object reqMsg = JProtoBufUtil.decode(reqGameMessagePackage.getBody(), reqMsgClass);

        Object res = methodAccess.invoke(action, method.getName(), context);
        if (res instanceof ResponseResult responseResult) {

            // GameMessageHeader clone = clone(header);
            // MessageTypeEnum messageType = cmd.isRpc() ? MessageTypeEnum.RPC_RESPONSE : MessageTypeEnum.RESPONSE;
            // clone.setMessageType(messageType);
            // clone.setSendTime(System.currentTimeMillis());
            // clone.setErrorCode(responseResult.getErrorCode());

            GameMessageHeader gameMessageHeader = cmd.createGameMessageHeader(false);
            gameMessageHeader.setSeq(header.getSeq());
            gameMessageHeader.setVersion(header.getVersion());
            gameMessageHeader.setPlayerId(header.getPlayerId());
            gameMessageHeader.setErrorCode(responseResult.getErrorCode());

            GameMessagePackage resGameMessagePackage = new GameMessagePackage();
            resGameMessagePackage.setHeader(gameMessageHeader);
            if (cmd.getResMsgClass() != null && responseResult.getResMsg() != null) {
                byte[] encode = JProtoBufUtil.encode(responseResult.getResMsg());
                resGameMessagePackage.setBody(encode);
            }

            MessageSender.sendToPlayer(resGameMessagePackage);
        }
    }
}
