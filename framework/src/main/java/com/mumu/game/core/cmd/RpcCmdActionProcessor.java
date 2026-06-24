package com.mumu.game.core.cmd;

import cn.hutool.core.lang.Assert;
import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.rpc.anno.RpcCmdMapping;
import com.mumu.game.core.utils.ModifierUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * RpcCmdActionProcessor
 * 解析 @CmdAction 和 @RpcCmdMapping 注解，生成 Action处理器
 * @author liuzhen
 * @version 1.0.0 2026/6/24 15:57
 */
@Component
public class RpcCmdActionProcessor {
    public void load(Map<Integer, CmdRegistrar> actionMap) {
        Map<String, Object> cmdActionMap = SpringContextUtils.getBeansWithAnnotation(CmdAction.class);
        int before = actionMap.size();
        for (Object object : cmdActionMap.values()) {
            scanCmdMapping(actionMap, object);
        }
        LogTopic.NET.info(
                "CmdActionRpcProcessor",
                "CmdAction",
                cmdActionMap.size(),
                "RpcCmdMapping",
                actionMap.size() - before);
    }

    /** 扫描 @RpcCmdMapping */
    private void scanCmdMapping(Map<Integer, CmdRegistrar> actionMap, Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            RpcCmdMapping mapping = method.getDeclaredAnnotation(RpcCmdMapping.class);
            if (mapping == null) {
                continue;
            }
            assertMethod(method);

            int cmd = mapping.value().getCmd();
            Assert.isFalse(actionMap.containsKey(cmd), "扫描到重复的rpcCmd: {}, 正在注册: {}", cmd, method);

            actionMap.put(cmd, new CmdRegistrar(cmd, object, method));
        }
    }

    private void assertMethod(Method method) {
        Assert.isTrue(ModifierUtil.isPublic(method), "方法声明必须是【public】: " + method);
        Class<?>[] params = method.getParameterTypes();
        Assert.isTrue(params.length == 1, "方法形参只能有【1】个: " + method);
        Assert.isTrue(
                ModifierUtil.isBelongTo(params[0], MessageContext.class),
                "方法形参必须是【MessageContext】:" + method);
    }
}
