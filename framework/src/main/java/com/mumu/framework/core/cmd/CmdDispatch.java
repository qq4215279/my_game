/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.framework.core.autoinit.AutoInitEvent;
import com.mumu.framework.core.autoinit.enums.AutoInitModule;
import com.mumu.framework.core.cmd.anno.CmdAction;
import com.mumu.framework.core.cmd.anno.CmdMapping;
import com.mumu.framework.core.cmd.enums.CmdManager;
import com.mumu.framework.core.game_netty.context.GameMessageContextImpl;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.util.SpringContextUtils;
import com.mumu.framework.core.util2.ModifierUtil;

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
    private Map<Integer, ActionInvocation> messageIdActionMap = Collections.emptyMap();

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
    private void scanCmdMapping(Map<Integer, ActionInvocation> actionMap, Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            CmdMapping mapping = method.getDeclaredAnnotation(CmdMapping.class);
            if (mapping == null) {
                continue;
            }
            assertMethod(method);

            int reqMessageId = CmdManager.getReqMessageId(mapping.value());
            Assert.isFalse(actionMap.containsKey(reqMessageId), "扫描到重复的cmd: {}, 正在注册: {}", reqMessageId, method);

            actionMap.put(reqMessageId, new ActionInvocation(reqMessageId, object, method));
        }
    }

    public void callMethod(GameMessageContextImpl messageContext) {
        // TODO
        long playerId = messageContext.getPlayerId();
        GameMessageHeader header = messageContext.getReqGameMessagePackage().getHeader();
        Integer messageId = header.getMessageId();
        ActionInvocation actionInvocation = messageIdActionMap.getOrDefault(messageId, null);
        if (actionInvocation == null) {
            LogTopic.NET.error("callMethod fail", "actionInvocation is null", "playerId", playerId);
            return;
        }
        actionInvocation.callMethod(messageContext);
    }

    private void assertMethod(Method method) {
        Assert.isTrue(ModifierUtil.isPublic(method), "方法声明必须是【public】: " + method);
        Class<?>[] params = method.getParameterTypes();
        Assert.isTrue(params.length == 1, "方法形参只能有【1】个: " + method);
        // Assert.isTrue(ModifierUtil.isBelongTo(params[0], MessageContext.class), "方法形参必须是【MessageContext】:" + method);
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }

    @Override
    public int order() {
        return 1;
    }
}
