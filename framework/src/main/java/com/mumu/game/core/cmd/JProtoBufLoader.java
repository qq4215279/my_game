package com.mumu.game.core.cmd;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.AutoInitManager;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.thread.ScheduledExecutorUtil;
import com.mumu.game.core.utils.JProtoBufUtil;

import java.util.Collection;
import java.util.List;

/**
 * JProtoBufLoader
 * JProtoBuf 加载
 * @author liuzhen
 * @version 1.0.0 2026/5/4 21:30
 */
public class JProtoBufLoader implements AutoInitEvent {

    @Override
    public void autoInit() {
        // test
        if ("1".equals("1")) {
            ScheduledExecutorUtil.execute(JProtoBufLoader::preLoadJProtoMsg);
        }
    }

    /** 预加载proto协议 */
    private static void preLoadJProtoMsg() {
        int loadCount = 0;
        for (Class<?> clazz : AutoInitManager.CLASSES) {
            if (clazz.isAnnotationPresent(ProtobufClass.class)
                    && !clazz.isEnum()
                    && !clazz.isInterface()) {
                loadCount++;
                // LogTopic.NET.debug(ConfigSwitchEnum.LOG_CMD, "JProtoBuf perLoad", "class", clazz.getSimpleName());
                // 提前创建协议的codec，提高运行时效率
                JProtoBufUtil.createCodec(clazz);
            }
        }
        LogTopic.NET.info("JProtoBuf perLoad 完成", "size", loadCount);
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.CORE;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public Collection<ServiceType> loadService() {
        return List.of(ServiceType.NONE);
    }
}