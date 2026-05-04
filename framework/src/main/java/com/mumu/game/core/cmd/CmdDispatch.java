/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.cmd.anno.CmdMapping;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.CmdManager;
import com.mumu.game.core.game_netty.context.GameMessageContextImpl;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.utils.ModifierUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * CmdDispatch
 * cmd 的分发器
 * @author liuzhen
 * @version 1.0.0 2025/3/30 18:07
 */
@Slf4j
@Component
public class CmdDispatch implements AutoInitEvent {
    /** 加载完成 */
    private final AtomicBoolean loaded = new AtomicBoolean();
    /** 加载的 action 集合 */
    private Map<Cmd, ActionInvocation> messageIdActionMap = Collections.emptyMap();

    @Override
    public void autoInit() {
        load();

    }

    private void load() {
        Map<String, Object> cmdActionMap = SpringContextUtils.getBeansWithAnnotation(CmdAction.class);
        int before = messageIdActionMap.size();
        for (Object object : cmdActionMap.values()) {
            scanCmdMapping(messageIdActionMap, object);
        }
        this.loaded.set(true);

        LogTopic.NET.info(
                "CmdActionProcessor",
                "CmdAction",
                cmdActionMap.size(),
                "CmdMapping",
                messageIdActionMap.size() - before);
    }

    /** 扫描 @CmdMapping */
    private void scanCmdMapping(Map<Cmd, ActionInvocation> actionMap, Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            CmdMapping mapping = method.getDeclaredAnnotation(CmdMapping.class);
            if (mapping == null) {
                continue;
            }
            assertMethod(method);

            Cmd cmd = mapping.value();
            Assert.isFalse(actionMap.containsKey(cmd), "扫描到重复的cmd: {}, 正在注册: {}", cmd, method);

            actionMap.put(cmd, new ActionInvocation(cmd, object, method));
        }
    }

    @Deprecated
    public void invokeMethod(GameMessageContextImpl gameMessageContext) {
        // TODO
        long playerId = gameMessageContext.getPlayerId();
        GameMessageHeader header = gameMessageContext.getReqGameMessagePackage().getHeader();
        Integer messageId = header.getMessageId();
        Cmd cmd = CmdManager.getCmd(messageId);
        if (cmd == null) {
            LogTopic.NET.error("invokeMethod fail", "cmd is null", "playerId", playerId);
            return;
        }
        ActionInvocation actionInvocation = messageIdActionMap.getOrDefault(cmd, null);
        if (actionInvocation == null) {
            LogTopic.NET.error("invokeMethod fail", "actionInvocation is null", "playerId", playerId);
            return;
        }
        actionInvocation.invokeMethod(gameMessageContext);
    }

    /** 断言方法 */
    private void assertMethod(Method method) {
        Assert.isTrue(ModifierUtil.isPublic(method), "方法声明必须是【public】: " + method);
        Class<?>[] params = method.getParameterTypes();
        Assert.isTrue(params.length == 1, "方法形参只能有【1】个: " + method);
        // Assert.isTrue(ModifierUtil.isBelongTo(params[0], MessageContext.class), "方法形参必须是【MessageContext】:" + method);
    }

    /**
     * 调用接口
     * @param context 消息上下文
     */
    public void invokeMethod(MessageContext context) {
        GameMessagePackage messagePackage = context.getMessagePackage();
        GameMessageHeader header = messagePackage.getHeader();

        long playerId = header.getPlayerId();
        Integer messageId = header.getMessageId();
        Cmd cmd = CmdManager.getCmd(messageId);
        if (cmd == null) {
            LogTopic.NET.error("invokeMethod fail", "cmd is null", "playerId", playerId);
            return;
        }
        ActionInvocation actionInvocation = messageIdActionMap.getOrDefault(cmd, null);
        if (actionInvocation == null) {
            LogTopic.NET.error("callMethod fail", "actionInvocation is null", "playerId", playerId);
            return;
        }
        actionInvocation.invokeMethod(context);
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }

    @Override
    public int order() {
        return 1;
    }

    public boolean inSelf(Cmd cmd) {
        return messageIdActionMap.containsKey(cmd);
    }
}
